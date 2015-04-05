package com.datamelt.kettle.jareclient;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
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

public class JareClientPluginMeta extends BaseStepMeta implements StepMetaInterface
{
	private String server;
	private String serverPort;
	
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
		ValueMetaInterface totalGroups=new ValueMeta("ruleengine_groups", ValueMeta.TYPE_INTEGER);
		totalGroups.setOrigin(origin);
		rowMeta.addValueMeta( totalGroups );
		
		ValueMetaInterface totalGroupsFailed=new ValueMeta("ruleengine_groups_failed", ValueMeta.TYPE_INTEGER);
		totalGroupsFailed.setOrigin(origin);
		rowMeta.addValueMeta( totalGroupsFailed );
		
		ValueMetaInterface totalRules=new ValueMeta("ruleengine_rules", ValueMeta.TYPE_INTEGER);
		totalRules.setOrigin(origin);
		rowMeta.addValueMeta( totalRules );
		
		ValueMetaInterface totalRulesFailed=new ValueMeta("ruleengine_rules_failed", ValueMeta.TYPE_INTEGER);
		totalRulesFailed.setOrigin(origin);
		rowMeta.addValueMeta( totalRulesFailed );
		
		ValueMetaInterface totalActions=new ValueMeta("ruleengine_actions", ValueMeta.TYPE_INTEGER);
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
