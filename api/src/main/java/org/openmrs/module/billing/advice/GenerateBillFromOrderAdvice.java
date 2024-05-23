package org.openmrs.module.billing.advice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Provider;
import org.openmrs.TestOrder;
import org.openmrs.User;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.IBillService;
import org.openmrs.module.billing.api.IBillableItemsService;
import org.openmrs.module.billing.api.ICashPointService;
import org.openmrs.module.billing.api.ItemPriceService;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillStatus;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceStatus;
import org.openmrs.module.billing.api.model.CashPoint;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.billing.api.search.BillableServiceSearch;
import org.openmrs.module.billing.exemptions.BillingExemptions;
import org.openmrs.module.billing.util.Utils;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateBillFromOrderAdvice implements AfterReturningAdvice {

	private static final Log LOG = LogFactory.getLog(GenerateBillFromOrderAdvice.class);
	
	OrderService orderService = Context.getOrderService();
	
	IBillService billService = Context.getService(IBillService.class);
	
	StockManagementService stockService = Context.getService(StockManagementService.class);
	
	ItemPriceService priceService = Context.getService(ItemPriceService.class);
	
	ICashPointService cashPointService = Context.getService(ICashPointService.class);
	
	/**
	 * This is called immediately an order is saved
	 */
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		// This advice will be executed after the saveOrder method is successfully called
		try {
			// Extract the Order object from the arguments
			ProgramWorkflowService workflowService = Context.getProgramWorkflowService();
			if (method.getName().equals("saveOrder") && args.length > 0 && args[0] instanceof Order) {
				Order order = (Order) args[0];
				
				// Check if the order already exists by looking at the database
				// Exclude discontinuation orders as well
				if (order.getAction().equals(Order.Action.DISCONTINUE) || order.getAction().equals(Order.Action.REVISE)
				        || order.getAction().equals(Order.Action.RENEW)) {
					// Do nothing unless order is new
					return;
				}
				
				// This is a new order
				Patient patient = order.getPatient();
				String cashierUUID = Context.getAuthenticatedUser().getUuid();
				String cashpointUUID = Utils.getDefaultLocation().getUuid();
				if (order instanceof DrugOrder) {
					DrugOrder drugOrder = (DrugOrder) order;
					Integer drugID = drugOrder.getDrug() != null ? drugOrder.getDrug().getDrugId() : 0;
					double drugQuantity = drugOrder.getQuantity() != null ? drugOrder.getQuantity() : 0.0;
					List<StockItem> stockItems = stockService.getStockItemByDrug(drugID);
					
					if (!stockItems.isEmpty()) {
						// check from the list for all exemptions
						boolean isExempted = checkIfOrderIsExempted(workflowService, order, BillingExemptions.COMMODITIES);
						BillStatus lineItemStatus = isExempted ? BillStatus.EXEMPTED : BillStatus.PENDING;
						addBillItemToBill(order, patient, cashierUUID, cashpointUUID, stockItems.get(0), null,
						    (int) drugQuantity, order.getDateActivated(), lineItemStatus);
					}
				} else if (order instanceof TestOrder) {
					TestOrder testOrder = (TestOrder) order;
					BillableService searchTemplate = new BillableService();
					searchTemplate.setConcept(testOrder.getConcept());
					searchTemplate.setServiceStatus(BillableServiceStatus.ENABLED);
					
					IBillableItemsService service = Context.getService(IBillableItemsService.class);
					List<BillableService> searchResult = service.findServices(new BillableServiceSearch(searchTemplate));
					if (!searchResult.isEmpty()) {
						boolean isExempted = checkIfOrderIsExempted(workflowService, order, BillingExemptions.SERVICES);
						BillStatus lineItemStatus = isExempted ? BillStatus.EXEMPTED : BillStatus.PENDING;
						addBillItemToBill(order, patient, cashierUUID, cashpointUUID, null, searchResult.get(0), 1,
						    order.getDateActivated(), lineItemStatus);
						
					}
				}
			}
		}
		catch (Exception e) {
			LOG.error("Error intercepting order before creation: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Checks if an order concept is in the exemptions list
	 *
	 * @param workflowService
	 * @param order
	 * @param config
	 * @return
	 */
	private boolean checkIfOrderIsExempted(ProgramWorkflowService workflowService, Order order,
	        Map<String, Set<Integer>> config) {
		if (config == null || order == null || config.size() == 0) {
			return false;
		}
		if (config.get("all") != null && config.get("all").contains(order.getConcept().getConceptId())) {
			return true;
		}
		// check in programs list
		List<String> programExemptions = config.keySet().stream().filter(key -> key.startsWith("program:"))
		        .collect(Collectors.toList());
		if (programExemptions.size() > 0) {
			List<PatientProgram> programs = workflowService.getPatientPrograms(order.getPatient(), null, null, null,
			    new Date(), null, false);
			Set<String> activeEnrollments = new HashSet<>();
			programs.forEach(patientProgram -> {
				if (patientProgram.getActive()) {
					activeEnrollments.add(patientProgram.getProgram().getName());
				}
			});
			
			for (String programEntry : programExemptions) {
				if (programEntry.contains(":")) { // this is our convention to distinguish program exemption
					String programName = programEntry.substring(programEntry.indexOf(":") + 1);
					//check if patient is active in the program
					if (activeEnrollments.contains(programName)) {
						// check if order is exempted
						if (config.get(programEntry).contains(order.getConcept().getConceptId())) {
							return true;
						}
						
					}
				}
			}
		}
		
		// check age category
		if (order.getPatient().getAge() < 5 && config.get("age<5") != null
		        && config.get("age<5").contains(order.getConcept().getConceptId())) {
			return true;
		}
		return false;
	}
	
	/**
	 * Adds a bill item to the cashier module
	 *
	 * @param patient
	 * @param cashierUUID
	 * @param cashpointUUID
	 */
	public void addBillItemToBill(Order order, Patient patient, String cashierUUID, String cashpointUUID,
	        StockItem stockitem, BillableService service, Integer quantity, Date orderDate, BillStatus lineItemStatus) {
		try {
			// Search for a bill
			Bill activeBill = new Bill();
			activeBill.setPatient(patient);
			activeBill.setStatus(BillStatus.PENDING);
			//            BillSearch billSearch = new BillSearch(searchBill);
			//            List<Bill> bills = billService.getBills(billSearch);
			//            Bill activeBill = null;
			//
			//            //search for any pending bill for today
			//            for (Bill currentBill : bills) {
			//                //Get the bill date
			//                if(DateUtils.isSameDay(currentBill.getDateCreated(), orderDate != null ? orderDate : new Date())) {
			//                    activeBill = currentBill;
			//                    break;
			//                }
			//            }
			//
			//            // if there is no active bill for today, we create one
			//            if(activeBill == null){
			//                activeBill = searchBill;
			//            }
			
			// Bill Item
			BillLineItem billLineItem = new BillLineItem();
			List<CashierItemPrice> itemPrices = new ArrayList<>();
			if (stockitem != null) {
				billLineItem.setItem(stockitem);
				itemPrices = priceService.getItemPrice(stockitem);
			} else if (service != null) {
				billLineItem.setBillableService(service);
				itemPrices = priceService.getServicePrice(service);
			}
			
			if (!itemPrices.isEmpty()) {
				//List<CashierItemPrice> matchingPrices = itemPrices.stream().filter(p -> p.getPaymentMode().getUuid().equals(fetchPatientPayment(order))).collect(Collectors.toList());
				// billLineItem.setPrice(matchingPrices.isEmpty() ? itemPrices.get(0).getPrice() : matchingPrices.get(0).getPrice());
				billLineItem.setPrice(itemPrices.get(0).getPrice());
			} else {
				billLineItem.setPrice(new BigDecimal(0.0));
			}
			billLineItem.setQuantity(quantity);
			billLineItem.setPaymentStatus(lineItemStatus);
			billLineItem.setLineItemOrder(0);
			billLineItem.setOrder(order);
			
			// Bill
			User user = Context.getAuthenticatedUser();
			List<Provider> providers = new ArrayList<>(Context.getProviderService().getProvidersByPerson(user.getPerson()));
			
			if (!providers.isEmpty()) {
				activeBill.setCashier(providers.get(0));
				List<CashPoint> cashPoints = cashPointService.getAll();
				activeBill.setCashPoint(cashPoints.get(0));
				activeBill.addLineItem(billLineItem);
				activeBill.setStatus(BillStatus.PENDING);
				billService.save(activeBill);
			} else {
				LOG.error("User is not a provider");
			}
			
		}
		catch (Exception ex) {
			LOG.error("Error sending the bill item: " + ex.getMessage(), ex);			
		}
	}
}
