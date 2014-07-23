package com.datamelt.kettle.jare;

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

public class JarePluginMeta extends BaseStepMeta implements StepMetaInterface
{
	private String ruleFilename;
	private String stepMain;
	private String stepRuleResults;
	private int outputType;
	
	public JarePluginMeta()
	{
		super(); // allocate BaseStepInfo
		
	}

	/**
	 * @return Returns the value.
	 */
	public String getRuleFileName()
	{
		return ruleFilename;
	}
	
	/**
	 * @param value The value to set.
	 */
	public void setStepMain(String name)
	{
		this.stepMain = name;
	}
	
	/**
	 * @return Returns the value.
	 */
	public String getStepMain()
	{
		return stepMain;
	}
	
	/**
	 * @param value The value to set.
	 */
	public void setStepRuleResults(String name)
	{
		this.stepRuleResults = name;
	}
	
	/**
	 * @return Returns the value.
	 */
	public String getStepRuleResults()
	{
		return stepRuleResults;
	}
	
	/**
	 * @param value The value to set.
	 */
	public void setRuleFileName(String ruleFilename)
	{
		this.ruleFilename = ruleFilename;
	}
	
	/**
	 * @return Returns the value.
	 */
	public int getOutputType()
	{
		return outputType;
	}
	
	/**
	 * @param value The value to set.
	 */
	public void setOutputType(int outputType)
	{
		this.outputType = outputType;
	}
	
	public String getXML() throws KettleException
	{
		StringBuffer retval = new StringBuffer(150);
        
        retval.append("    ").append(XMLHandler.addTagValue("rule_file_name", ruleFilename));
        retval.append("    ").append(XMLHandler.addTagValue("rule_step_main", stepMain));
        retval.append("    ").append(XMLHandler.addTagValue("rule_step_rule_results", stepRuleResults));
        retval.append("    ").append(XMLHandler.addTagValue("output_type", outputType));
        return retval.toString();
	}

	public Object clone()
	{
		//Object retval = super.clone();
		JarePluginMeta retval = (JarePluginMeta) super.clone();
		retval.ruleFilename= ruleFilename;
		retval.stepMain = stepMain;
		return retval;
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String,Counter> counters)
		throws KettleXMLException
	{
		try
		{
			ruleFilename =  XMLHandler.getTagValue(stepnode, "rule_file_name");
			stepMain =  XMLHandler.getTagValue(stepnode, "rule_step_main");
			stepRuleResults =  XMLHandler.getTagValue(stepnode, "rule_step_rule_results");
			outputType =  Integer.parseInt(XMLHandler.getTagValue(stepnode, "output_type"));

		}
		catch(Exception e)
		{
			throw new KettleXMLException("Unable to read rule engine step info from XML node", e);
		}
	}

	public void setDefault()
	{
		ruleFilename = "";
		stepMain = "";
		stepRuleResults = Messages.getString("JarePluginDialog.Step.RuleResults.Type");
		outputType=0;
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException
	{
		//depending on who calls this method (preview, show output/input fields, etc)
		// we need to give back the appropriate information
		if( nextStep==null || nextStep.getName().equals(stepMain))
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
		else if(nextStep.getName().equals(stepRuleResults))
		{
			ValueMetaInterface group = new ValueMeta("ruleengine_group", ValueMeta.TYPE_STRING);
			group.setOrigin(origin);
			rowMeta.addValueMeta( group );
			
			ValueMetaInterface groupFailed = new ValueMeta("ruleengine_group_failed", ValueMeta.TYPE_INTEGER);
			groupFailed.setOrigin(origin);
			rowMeta.addValueMeta( groupFailed );
			
			ValueMetaInterface subgroup = new ValueMeta("ruleengine_subgroup", ValueMeta.TYPE_STRING);
			subgroup.setOrigin(origin);
			rowMeta.addValueMeta( subgroup );

			ValueMetaInterface subgroupFailed = new ValueMeta("ruleengine_subgroup_failed", ValueMeta.TYPE_INTEGER);
			subgroupFailed.setOrigin(origin);
			rowMeta.addValueMeta( subgroupFailed );
			
			ValueMetaInterface subgroupIntergroupOperator = new ValueMeta("ruleengine_subgroup_intergroup_operator", ValueMeta.TYPE_STRING);
			subgroupIntergroupOperator.setOrigin(origin);
			rowMeta.addValueMeta( subgroupIntergroupOperator );
			
			ValueMetaInterface subgroupRuleOperator = new ValueMeta("ruleengine_subgroup_rule_operator", ValueMeta.TYPE_STRING);
			subgroupRuleOperator.setOrigin(origin);
			rowMeta.addValueMeta( subgroupRuleOperator );
			
			ValueMetaInterface rule = new ValueMeta("ruleengine_rule", ValueMeta.TYPE_STRING);
			rule.setOrigin(origin);
			rowMeta.addValueMeta( rule );
			
			ValueMetaInterface ruleFailed = new ValueMeta("ruleengine_rule_failed", ValueMeta.TYPE_INTEGER);
			ruleFailed.setOrigin(origin);
			rowMeta.addValueMeta( ruleFailed );
			
			ValueMetaInterface ruleMessage = new ValueMeta("ruleengine_message", ValueMeta.TYPE_STRING);
			ruleMessage.setOrigin(origin);
			rowMeta.addValueMeta( ruleMessage );
		}
	}

	public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String,Counter> counters) throws KettleException
	{
		try
		{
			ruleFilename = rep.getStepAttributeString(id_step, "rule_file_name");
			stepMain = rep.getStepAttributeString(id_step, "rule_step_main");
			stepRuleResults = rep.getStepAttributeString(id_step, "rule_step_rule_results");
			outputType = Integer.parseInt(rep.getStepAttributeString(id_step, "output_type"));
			
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("error reading rule engine step with id_step="+id_step+" from the repository", dbe);
		}
		catch(Exception e)
		{
			throw new KettleException("Unexpected error reading rule engine step with id_step="+id_step+" from the repository", e);
		}
	}
	
	public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "rule_file_name", ruleFilename);
			rep.saveStepAttribute(id_transformation, id_step, "rule_step_main", stepMain);
			rep.saveStepAttribute(id_transformation, id_step, "rule_step_rule_results", stepRuleResults);
			rep.saveStepAttribute(id_transformation, id_step, "output_type", outputType);
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException("Unable to save rule engine step information to the repository, id_step="+id_step, dbe);
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
		// See if we have a main output stream 
	    if (stepMain!=null && stepMain.length()>0) 
	    {
	        cr = new CheckResult(CheckResult.TYPE_RESULT_OK, "Main output step is defined", stepMeta);
	        remarks.add(cr);
	    } else 
	    {
	        cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, "Main output step undefined", stepMeta);
	        remarks.add(cr);
	    }
	    // See if we have a rule results output stream 
	    if (stepRuleResults!=null && stepRuleResults.length()>0 && !stepRuleResults.equals(Messages.getString("JarePluginDialog.Step.RuleResults.Type")))
	    {
	        cr = new CheckResult(CheckResult.TYPE_RESULT_OK, "Rule Results output step is defined", stepMeta);
	        remarks.add(cr);
	    } else 
	    {
	        cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING, "Rule results output step undefined", stepMeta);
	        remarks.add(cr);
	    } 
	}
	
	public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name)
	{
		return new JarePluginDialog(shell, meta, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp)
	{
		return new JarePlugin(stepMeta, stepDataInterface, cnr, transMeta, disp);
	}

	public StepDataInterface getStepData()
	{
		return new JarePluginData();
	}
	
	@Override
	public boolean excludeFromCopyDistributeVerification()
	{
		return true;
	}
	
}
