/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */ 
package com.datamelt.kettle.jareclient;

import java.math.BigDecimal;
import java.util.Date;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.datamelt.server.RuleEngineClient;
import com.datamelt.server.RuleEngineServerObject;
import com.datamelt.util.HeaderRow;
import com.datamelt.util.RowField;
import com.datamelt.util.RowFieldCollection;

/**
 * This is the client server step. The rule engine will run on
 * a server and this plugin is the client communicating with the
 * server.
 * 
 * Plugin to check data of incomming rows against business rules
 * defined in one or multiple xml files or a zip file. 
 * 
 * uses JaRE - Java Rule Engine of datamelt.com
 * 
 * Adds various fields to the output row identifying the number of
 * groups, groups failed, number of rules, rules failed and number
 * of actions.
 * 
 * @author uwe geercken - uwe.geercken@web.de
 * 
 * version 0.3 
 * last update: 2017-11-14 
 */

public class JareClientPlugin extends BaseStep implements StepInterface
{
	// fields added to each output row for the main output
	private static final String FIELDNAME_RULEENGINE_GROUPS 		= "ruleengine_groups";
	private static final String FIELDNAME_RULEENGINE_GROUPS_FAILED 	= "ruleengine_groups_failed";
	private static final String FIELDNAME_RULEENGINE_GROUPS_SKIPPED = "ruleengine_groups_skipped";
	private static final String FIELDNAME_RULEENGINE_RULES 			= "ruleengine_rules";
	private static final String FIELDNAME_RULEENGINE_RULES_FAILED 	= "ruleengine_rules_failed";
	private static final String FIELDNAME_RULEENGINE_ACTIONS 		= "ruleengine_actions";

    private JareClientPluginData data;
	private JareClientPluginMeta meta;
	
	private RowMetaInterface inputRowMeta;
	
	private RuleEngineClient client = null;
	private static HeaderRow header;
	private int inputSize=0;
	
	public JareClientPlugin(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis)
	{
		super(s,stepDataInterface,c,t,dis);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta = (JareClientPluginMeta)smi;
	    data = (JareClientPluginData)sdi;
	    
	    // get the row
		Object[] r=getRow();
		
		// if no more rows, we are done
		if (r==null)
		{
			try
			{
				// close the output stream and socket to server
				client.getServerObject("exit");
				client.closeOutputStream();
				client.closeSocket();
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
			setOutputDone();
			return false;
		}
		// only done on the first row
		if (first)
        {
			// number of fields of the input row
            inputSize = getInputRowMeta().size();
            
            // for main output step
            data.outputRowMeta = (RowMetaInterface)getInputRowMeta().clone();
            addFieldsToRowMeta(data.outputRowMeta, getStepname());
            
            inputRowMeta = getInputRowMeta();
            // names of the header fields
            header = new HeaderRow(inputRowMeta.getFieldNames());
            
            String serverName = environmentSubstitute(meta.getServer());
            String serverPort = environmentSubstitute(meta.getServerPort());
            try
            {
            	// create client connection to server
            	client = new RuleEngineClient(serverName,Integer.parseInt(serverPort));
            }
            catch(Exception ex)
            {
            	log.logError("error creating ruleengine client instance",ex.fillInStackTrace());
            }
            
            first = false;
        }

        // generate output row, make it correct size
        Object[] outputRow = RowDataUtil.resizeArray(r, data.outputRowMeta.size());
        
        // object/collection that holds all the fields and their values required for running the rule engine
        RowFieldCollection fields = new RowFieldCollection(header,r);
        
        // send row to server and receive the result
        try
        {
        	RuleEngineServerObject response = client.getServerObject(fields);
        	// set the output row fields to the values received by the rule engine server
        	outputRow[inputSize] = (long)response.getTotalGroups();
        	outputRow[inputSize+1] = (long)response.getTotalGroups() - (long)response.getGroupsPassed();
        	outputRow[inputSize+2] = (long)response.getGroupsSkipped();
        	outputRow[inputSize+3] = (long)response.getTotalRules();
        	outputRow[inputSize+4] = (long)response.getTotalRules() - (long)response.getRulesPassed();
        	outputRow[inputSize+5] = (long)response.getTotalActions();
        	
        }
        catch(Exception ex)
        {        	
       		log.logError("error receiving object from ruleengine server", ex.fillInStackTrace());
       		setStopped(true);
       		setOutputDone();
       		setErrors(1);
       		stopAll();
       		
        	return false;
        }
        
        // process only updated fields by the rule engine
        // if there have been actions defined in the rule files
        try
        {
        	// process only if the collection of fields was changed
        	if(fields.isCollectionUpdated())
        	{
	        	for(int i=0;i<inputSize;i++)
	            {
	           		ValueMetaInterface vmi = inputRowMeta.searchValueMeta(header.getFieldName(i));
	           		int fieldType = vmi.getType();
	           		RowField rf = fields.getField(i);
	           		// if the field has been updated, then get the value appropriate to the type
	           		if(rf.isUpdated())
	           		{
	           			log.logRowlevel("field: " + rf.getName() + " [" + fieldType + "] updated from rule engine");
	           			if(fieldType == ValueMetaInterface.TYPE_BOOLEAN)
		           		{
		           			outputRow[i] = rf.getValue();
		           		}
	           			else if(fieldType == ValueMetaInterface.TYPE_STRING)
		           		{
		           			outputRow[i] = rf.getValue();
		           		}
		           		else if(fieldType == ValueMetaInterface.TYPE_INTEGER) 
		           		{
		           			outputRow[i] = (Long)rf.getValue();
		           		}
		           		else if(fieldType == ValueMetaInterface.TYPE_NUMBER)
		           		{
		           			if(rf.getValue() instanceof Long)
		           			{
		           				outputRow[i] = ((Long)rf.getValue()).doubleValue();
		           			}
		           			else if(rf.getValue() instanceof Double)
		           			{
		           				outputRow[i] = rf.getValue();
		           			}
		           			else if(rf.getValue() instanceof Integer)
		           			{
		           				outputRow[i] = ((Integer)rf.getValue()).doubleValue();
		           			}
		           		}
		           		else if(fieldType == ValueMetaInterface.TYPE_BIGNUMBER) 
		           		{
		           			if(rf.getValue() instanceof Long)
		           			{
		           				outputRow[i] = new BigDecimal((Long)rf.getValue());
		           			}
		           			else if(rf.getValue() instanceof Double)
		           			{
		           				outputRow[i] = new BigDecimal((Double)rf.getValue());
		           			}
		           			else if(rf.getValue() instanceof Integer)
		           			{
		           				outputRow[i] = new BigDecimal((Integer)rf.getValue());
		           			} 
		           		}
		           		else if(fieldType == ValueMetaInterface.TYPE_DATE)
		           		{
		           			outputRow[i] = (Date)rf.getValue(); 
		           		}
		           		else
		           		{
		           			throw new Exception("invalid output field type: " + fieldType);
		           		}
	           		}
	            }
        	}
        }
        catch(Exception ex)
        {
       		log.logError("error updating output fields", ex.fillInStackTrace());
       		setStopped(true);
       		setOutputDone();
       		setErrors(1);
       		stopAll();
        	return false;
        }
        
        putRow(data.outputRowMeta, outputRow);
		return true;
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (JareClientPluginMeta)smi;
	    data = (JareClientPluginData)sdi;

	    return super.init(smi, sdi);
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (JareClientPluginMeta)smi;
	    data = (JareClientPluginData)sdi;

	    super.dispose(smi, sdi);
	}
	
	public void run()
	{
		try
		{
			while (processRow(meta, data) && !isStopped());
		}
		catch(Exception e)
		{
			logError("Unexpected error : "+e.toString());
			setErrors(1);
			stopAll();
		}
		finally
		{
		    dispose(meta, data);
			logBasic("Finished, processing "+ getLinesRead() + " input rows");
			markStop();
		}
	}

	private void addFieldsToRowMeta(RowMetaInterface r, String origin)
	{
		ValueMetaInterface totalGroups=new ValueMetaInteger(FIELDNAME_RULEENGINE_GROUPS);
		totalGroups.setOrigin(origin);
		r.addValueMeta( totalGroups );
		
		ValueMetaInterface totalGroupsFailed=new ValueMetaInteger(FIELDNAME_RULEENGINE_GROUPS_FAILED);
		totalGroupsFailed.setOrigin(origin);
		r.addValueMeta( totalGroupsFailed );
		
		ValueMetaInterface totalGroupSkipped = new ValueMetaInteger(FIELDNAME_RULEENGINE_GROUPS_SKIPPED);
		totalGroupSkipped.setOrigin(origin);
		r.addValueMeta( totalGroupSkipped );
		
		ValueMetaInterface totalRules=new ValueMetaInteger(FIELDNAME_RULEENGINE_RULES);
		totalRules.setOrigin(origin);
		r.addValueMeta( totalRules );
		
		ValueMetaInterface totalRulesFailed=new ValueMetaInteger(FIELDNAME_RULEENGINE_RULES_FAILED);
		totalRulesFailed.setOrigin(origin);
		r.addValueMeta( totalRulesFailed );
		
		ValueMetaInterface totalActions=new ValueMetaInteger(FIELDNAME_RULEENGINE_ACTIONS);
		totalActions.setOrigin(origin);
		r.addValueMeta( totalActions );
		
	}
}
