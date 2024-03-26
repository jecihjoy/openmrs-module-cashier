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
package org.openmrs.module.billing.api.impl;

import org.openmrs.module.billing.api.IPaymentModeAttributeTypeService;
import org.openmrs.module.billing.api.base.entity.impl.BaseMetadataDataServiceImpl;
import org.openmrs.module.billing.api.base.entity.security.IMetadataAuthorizationPrivileges;
import org.openmrs.module.billing.api.model.PaymentModeAttributeType;
import org.openmrs.module.billing.api.security.BasicMetadataAuthorizationPrivileges;

/**
 * Data service implementation class for {@link PaymentModeAttributeType}s.
 */
public class PaymentModeAttributeTypeServiceImpl extends BaseMetadataDataServiceImpl<PaymentModeAttributeType> implements IPaymentModeAttributeTypeService {
	
	@Override
	protected IMetadataAuthorizationPrivileges getPrivileges() {
		return new BasicMetadataAuthorizationPrivileges();
	}
	
	@Override
	protected void validate(PaymentModeAttributeType entity) {
	}
}
