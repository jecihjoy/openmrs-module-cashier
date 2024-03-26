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
package org.openmrs.module.billing.api.base.compatibility;

/**
 * A layer to allow privilege constants to be selected and have their assignment of different
 * OpenMRS versions done elsewhere
 */
public abstract class PrivilegeConstantsCompatibility {
	
	/* Commons privilege constants */
	public static String GET_LOCATIONS = "";
	
	public static String GET_CONCEPT_CLASSES = "";
	
	public static String GET_USERS = "";
	
	public static String GET_PROVIDERS = "";
	
	public static String GET_CONCEPTS = "";
	
	/* Cashier Module privilege constants */
	public static String GET_VISITS = "";
	
	public static String GET_ENCOUNTERS = "";
	
	public static String GET_OBS = "";
	
	public static String GET_PATIENTS = "";
	
	public static String DASHBOARD_SUMMARY = "";
	
	public static String DASHBOARD_DEMOGRAPHICS = "";
	
	public static String DASHBOARD_OVERVIEW = "";
	
	public static String DASHBOARD_VISITS = "";
	
	/* Inventory Module privilege constants */
	public static String GET_ROLES = "";
}
