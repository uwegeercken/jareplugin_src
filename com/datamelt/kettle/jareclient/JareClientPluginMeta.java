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

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

@Step( id = "JareRuleEngineClient", image = "check_ok.svg",
i18nPackageName = "com.datamelt.kettle.jareclient", name = "JareClientPlugin.Step.Name",
description = "JareClientPlugin.Step.Description",
categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Validation",
documentationUrl = "https://github.com/uwegeercken/jareplugin/wiki/Rule-Engine-Client-Plugin" )

@InjectionSupported( localizationPrefix = "JarePluginDialog.Injection.")
public class JareClientPluginMeta extends BaseStepMeta implements StepMetaInterface
{
	@Injection( name = "SERVER" ) String server;
	@Injection( name = "SERVER_PORT" ) String serverPort;
	
	public JareClientPluginMeta()
	{
		super(); // allocate BaseStepInfo
		
	}

	/**
	 * @return Returns the value.
	 */
	public String getServer()
	{
		return server;
	}
	
	/**
	 * @param value The value to set.
	 */
	public void setServer(String server)
	{
		this.server = server;
	}
	
	public String getXML() throws KettleException
	{
		StringBuffer retval = new StringBuffer(150);
        
        retval.append("    ").append(XMLHandler.addTagValue("server", server));
        retval.append("    ").append(XMLHandler.addTagValue("server_port", serverPort));
        return retval.toString();
	}

	public Object clone()
	{
		//Object retval = super.clone();
		JareClientPluginMeta retval = (JareClientPluginMeta) super.clone();
		retval.server= server;
		retval.serverPort= serverPort;
		return retval;
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String,Counter> counters)
		throws KettleXMLException
	{
		try
		{
			server =  XMLHandler.getTagValue(stepnode, "server");
			serverPort =  XMLHandler.getTagValue(stepnode, "server_port");
		}
		catch(Exception e)
		{
			throw new KettleXMLException("Unable to read rule engine server step info from XML node", e);
		}
	}

	public void setDefault()
	{
		server = "";
		serverPort = "";
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException
	{
		//ValueMetaInterface totalGroups=new ValueMeta("ruleengine_groups", ValueMeta.TYPE_INTEGER);
		ValueMetaInterface totalGroups=new ValueMetaInteger("ruleengine_groups");
		totalGroups.setOrigin(origin);
		rowMeta.addValueMeta( totalGroups );
		
		//ValueMetaInterface totalGroupsFailed=new ValueMeta("ruleengine_groups_failed", ValueMeta.TYPE_INTEGER);
		ValueMetaInterface totalGroupsFailed=new ValueMetaInteger("ruleengine_groups_failed");
		totalGroupsFailed.setOrigin(origin);
		rowMeta.addValueMeta( totalGroupsFailed );
		
		//ValueMetaInterface totalRules=new ValueMeta("ruleengine_rules", ValueMeta.TYPE_INTEGER);
		ValueMetaInterface totalRules=new ValueMetaInteger("ruleengine_rules");
		totalRules.setOrigin(origin);
		rowMeta.addValueMeta( totalRules );
		
		//ValueMetaInterface totalRulesFailed=new ValueMeta("ruleengine_rules_failed", ValueMeta.TYPE_INTEGER);
		ValueMetaInterface totalRulesFailed=new ValueMetaInteger("ruleengine_rules_failed");
		totalRulesFailed.setOrigin(origin);
		rowMeta.addValueMeta( totalRulesFailed );
		
		//ValueMetaInterface totalActions=new ValueMeta("ruleengine_actions", ValueMeta.TYPE_INTEGER);
		ValueMetaInterface totalActions=new ValueMetaInteger("ruleengine_actions");
		totalActions.setOrigin(origin);
		rowMeta.addValueMeta( totalActions );
	}

	public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String,Counter> counters) throws KettleException
	{
		try
		{
			server = rep.getStepAttributeString(id_step, "server");
			serverPort = rep.getStepAttributeString(id_step, "server_port");
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("error reading rule engine server step with id_step="+id_step+" from the repository", dbe);
		}
		catch(Exception e)
		{
			throw new KettleException("Unexpected error reading rule engine server step with id_step="+id_step+" from the repository", e);
		}
	}
	
	public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "server", server);
			rep.saveStepAttribute(id_transformation, id_step, "server_port", serverPort);
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to save rule engine server step information to the repository, id_step="+id_step, dbe);
		}
	}

	public void check(List<CheckResultInterface> remarks, TransMeta transmeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;
		if (prev==null || prev.size()==0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, "Not receiving any fields from previous steps!", stepMeta);
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, "Step is connected to previous one, receiving "+prev.size()+" fields", stepMeta);
			remarks.add(cr);
		}
		
		// See if we have input streams leading to this step!
		if (input.length>0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, "Step is receiving info from other steps.", stepMeta);
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, "No input received from other steps!", stepMeta);
			remarks.add(cr);
		}
	}
	
	public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name)
	{
		return new JareClientPluginDialog(shell, meta, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp)
	{
		return new JareClientPlugin(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}

	public StepDataInterface getStepData()
	{
		return new JareClientPluginData();
	}
	
	@Override
	public boolean excludeFromCopyDistributeVerification()
	{
		return true;
	}

	public String getServerPort()
	{
		return serverPort;
	}

	public void setServerPort(String serverPort)
	{
		this.serverPort = serverPort;
	}
}
