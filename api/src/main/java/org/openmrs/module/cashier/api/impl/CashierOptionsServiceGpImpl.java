/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.cashier.api.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.cashier.ModuleSettings;
import org.openmrs.module.cashier.api.ICashierOptionsService;
import org.openmrs.module.cashier.api.model.CashierOptions;
import org.openmrs.module.stockmanagement.api.model.StockItem;

/**
 * Service to load CashierOptions from global options
 *
 * @author daniel
 */
public class CashierOptionsServiceGpImpl implements ICashierOptionsService {
	
	private static final Log LOG = LogFactory.getLog(CashierOptionsServiceGpImpl.class);
	
	public CashierOptionsServiceGpImpl() {
		
	}
	
	/**
	 * Loads the cashier options from the database.
	 *
	 * @return The {@link CashierOptions}
	 * @should throw APIException if rounding is set but rounding item is not
	 * @should throw APIException if rounding is set but rounding item cannot be found
	 * @should not throw exception if numeric options are null
	 * @should default to false if timesheet required is not specified
	 * @should load cashier options from the database
	 */
	public CashierOptions getOptions() {
		CashierOptions options = new CashierOptions();
		
		setDefaultReceiptReportId(options);
		//		setRoundingOptions(options);
		if (StringUtils.isEmpty(options.getRoundingItemUuid())) {
			setRoundingOptionsForEmptyUuid(options);
		}
		setTimesheetOptions(options);
		
		return options;
	}
	
	private void setRoundingOptions(CashierOptions options) {
		String roundingModeProperty = Context.getAdministrationService()
		        .getGlobalProperty(ModuleSettings.ROUNDING_MODE_PROPERTY);
		if (StringUtils.isNotEmpty(roundingModeProperty)) {
			try {
				options.setRoundingMode(CashierOptions.RoundingMode.valueOf(roundingModeProperty));
				
				String roundToNearestProperty = Context.getAdministrationService()
				        .getGlobalProperty(ModuleSettings.ROUND_TO_NEAREST_PROPERTY);
				if (StringUtils.isNotEmpty(roundToNearestProperty)) {
					options.setRoundToNearest(Integer.valueOf(roundToNearestProperty));
					
					String roundingItemId = Context.getAdministrationService()
					        .getGlobalProperty(ModuleSettings.ROUNDING_ITEM_ID);
					if (StringUtils.isNotEmpty(roundingItemId)) {
						StockItem roundingItem = new StockItem();
						try {
							Integer itemId = Integer.parseInt(roundingItemId);
							// TODO Rounding logic
							//							roundingItem = Context.getService(IItemDataService.class).getById(itemId);
						}
						catch (Exception e) {
							LOG.error("Did not find rounding item by ID with ID <" + roundingItemId + ">", e);
						}
						if (roundingItem != null) {
							options.setRoundingItemUuid(roundingItem.getUuid());
						} else {
							LOG.error("Rounding item is NULL. Check your ID");
						}
					}
				}
			}
			catch (IllegalArgumentException iae) {
				/* Use default if option is not set */
				LOG.error("IllegalArgumentException occured", iae);
			}
			catch (NullPointerException e) {
				/* Use default if option is not set */
				LOG.error("NullPointerException occured", e);
			}
		}
	}
	
	private void setDefaultReceiptReportId(CashierOptions options) {
		String receiptReportIdProperty = Context.getAdministrationService()
		        .getGlobalProperty(ModuleSettings.RECEIPT_REPORT_ID_PROPERTY);
		if (StringUtils.isNotEmpty(receiptReportIdProperty)) {
			try {
				options.setDefaultReceiptReportId(Integer.parseInt(receiptReportIdProperty));
			}
			catch (NumberFormatException e) {
				/* Leave unset; must be handled, e.g. in ReceiptController */
				LOG.error("Error parsing ReceiptReportId <" + receiptReportIdProperty + ">", e);
			}
		}
	}
	
	private void setRoundingOptionsForEmptyUuid(CashierOptions options) {
		options.setRoundingMode(CashierOptions.RoundingMode.MID);
		options.setRoundToNearest(0);
	}
	
	private void setTimesheetOptions(CashierOptions options) {
		String timesheetRequiredProperty = Context.getAdministrationService()
		        .getGlobalProperty(ModuleSettings.TIMESHEET_REQUIRED_PROPERTY);
		if (StringUtils.isNotBlank(timesheetRequiredProperty)) {
			try {
				options.setTimesheetRequired(Boolean.parseBoolean(timesheetRequiredProperty));
			}
			catch (Exception ex) {
				options.setTimesheetRequired(false);
			}
		} else {
			options.setTimesheetRequired(false);
		}
	}
}
