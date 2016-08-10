package com.datamelt.kettle.jare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.xml.sax.SAXException;

import com.datamelt.rules.core.RuleExecutionResult;
import com.datamelt.rules.core.RuleGroup;
import com.datamelt.rules.core.RuleSubGroup;
import com.datamelt.rules.core.XmlRule;
import com.datamelt.rules.engine.BusinessRulesEngine;
import com.datamelt.util.RowField;
import com.datamelt.util.RowFieldCollection;

/**
 * Plugin to check data of incomming rows against business rules
 * defined in one or multiple xml files. 
 * 
 * uses JaRE - Java Rule Engine of datamelt.com
 * 
 * Adds various fields to the output row identifying the number of
 * groups, groups failed, number of rules, rules failed and number
 * of actions.
 * 
 * The results of the rule engine can be written to an second step
 * which will show one line per rule. the output type will determine if
 * all rules are passed, failed ones or passed ones.
 *  
 * 
 * @author uwe geercken - uwe.geercken@web.de
 * 
 * version 0.31 
 * last update: 2016-08-10 
 */

public class JarePlugin extends BaseStep implements StepInterface
{
	private BusinessRulesEngine ruleEngine;
	
    private JarePluginData data;
	private JarePluginMeta meta;
	
	private RowMetaInterface inputRowMeta;
	
	private String[] fieldNames;
	private int inputSize=0;
	private String realFilename;
	
	public JarePlugin(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis)
	{
		super(s,stepDataInterface,c,t,dis);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta = (JarePluginMeta)smi;
	    data = (JarePluginData)sdi;
	    
	    // output for main step
	    RowSet rowsetMain =  findOutputRowSet(meta.getStepMain());
	    //output for rule engine results step
	    RowSet rowsetRuleResults = null;
	    
	    try
	    {
	    	// check that the rule results step is defined
	    	// and it is not set to "no output"
	    	// and it is not the same as the main output step
	    	if(meta.getStepRuleResults()!=null && !meta.getStepRuleResults().equals(Messages.getString("JarePluginDialog.Step.RuleResults.Type")) && ! meta.getStepRuleResults().equals(meta.getStepMain()))
	    	{
	    		rowsetRuleResults = findOutputRowSet(meta.getStepRuleResults());
	    	}
	    }
	    catch(Exception ex)
	    {
	    	// when there is no rule results step selected or null
	    	// nothing is output
	    }
	    
	    // get the row
		Object[] r=getRow();
		// if no more rows, we are done
		if (r==null)
		{
			setOutputDone();
			return false;
		}
		// only done on the first row
		if (first)
        {
			log.logDebug("processing first row");
			// number of fields of the input row
            inputSize = getInputRowMeta().size();
            
            // for main output step
            data.outputRowMeta = (RowMetaInterface)getInputRowMeta().clone();
            addFieldstoRowMeta(data.outputRowMeta, getStepname(), false);
            
            // for output step with rule results
            data.outputRowMetaRuleResults = (RowMetaInterface)getInputRowMeta().clone();
            addFieldstoRowMeta(data.outputRowMetaRuleResults, getStepname(), true);
            
            inputRowMeta = getInputRowMeta();
            // names of the fields
            fieldNames = inputRowMeta.getFieldNames();
            log.logDebug("found input fields: " + fieldNames.length);
            
            // filename of the rule engine xml file
            realFilename = getRealName(meta.getRuleFileName());
            try
            {
            	log.logDebug("trying to use file for the ruleengine: " + realFilename);
            	File f = new File(realFilename);
            	if(!f.exists())
            	{
            		throw new FileNotFoundException("the  specified file was not found: " + realFilename);
            	}
            	// we can use a zip file containing all rules
            	if(f.isFile() && realFilename.endsWith(".zip"))
            	{
            		log.logDebug("found zip file to read xml rule files: " + realFilename);
            		ZipFile zip = new ZipFile(realFilename);
            		ruleEngine = new BusinessRulesEngine(zip);
            	}
            	// we can also use a directory and read all files from there
            	else if(f.isDirectory())
            	{
            		// use a filter - we only want to read xml files
            		log.logDebug("found directory to read xml rules files: " + realFilename);
            		FilenameFilter fileNameFilter = new FilenameFilter()
            		{
                        @Override
                        public boolean accept(File dir, String name) {
                           if(name.lastIndexOf('.')>0)
                           {
                              // get last index for '.' char
                              int lastIndex = name.lastIndexOf('.');
                              
                              // get extension
                              String str = name.substring(lastIndex);
                              
                              // match path name extension
                              if(str.equals(".xml"))
                              {
                                 return true;
                              }
                           }
                           return false;
                        }
                     };
                     // get list of files for the given filter
            		File[] listOfFiles = f.listFiles(fileNameFilter);
            		// initialize rule engine with list of files
            		ruleEngine = new BusinessRulesEngine(listOfFiles);
            	}
            	else if(f.isFile())
            	{
            		log.logDebug("found single xml rules file: " + realFilename);
            		ruleEngine = new BusinessRulesEngine(realFilename);
            	}
            	log.logBasic("initialized business rule engine version: " + BusinessRulesEngine.getVersion() + " using: " + realFilename);
            }
            catch(SAXException se)
            {
            	log.logError(se.toString());
            	setStopped(true);
            	setOutputDone();
            	setErrors(1);
            	return false;
            }
            catch(FileNotFoundException fnf)
            {
            	log.logError(fnf.toString());
            	setStopped(true);
            	setOutputDone();
            	setErrors(1);
            	return false;
            }
            catch(Exception ex)
            {
            	log.logError("error initializing business rule engine with rule file: " + realFilename, ex.toString());
            	//log.logError(Const.getStackTracker(ex));
            	setStopped(true);
            	setOutputDone();
            	setErrors(1);
            	return false;
            }
            
            // if we do not output the detailed rule results, we do not store the
        	// execution results. this will speedup processing
        	if(rowsetRuleResults== null)
        	{
        		ruleEngine.setPreserveRuleExcecutionResults(false);
        	}
            first = false;
        }

        // generate output row, make it correct size
        Object[] outputRow = RowDataUtil.resizeArray(r, data.outputRowMeta.size());
        
        // generate output row for rule results, make it correct size
        Object[] outputRowRuleResults = RowDataUtil.resizeArray(r, data.outputRowMetaRuleResults.size());
        
        // object/collection that holds all the fields and their values required for running the rule engine
        RowFieldCollection fields = new RowFieldCollection();
        
        // all fields are passed to the rule engine. alternatively one could pass single fields
        // (for performance reasons)
        for(int i=0;i<fieldNames.length;i++) 
        {
        	if(outputRow[i]!=null)
        	{
        		fields.addField(fieldNames[i],outputRow[i]);
        	}
        	else
        	{
        		fields.addField(fieldNames[i],null);  
        	}
        }
        log.logDebug("number of fields: " + fields.getNumberOfFields());
        // run the rule engine
        try
        {
        	log.logDebug("running the ruleengine");
        	ruleEngine.run("row number: " + getLinesRead() ,fields);
        	if(log.isDetailed())
        	{
        		for(int i=0;i<ruleEngine.getGroups().size();i++)
        		{
   					String message = "line: " +getLinesRead() + ", group: " + ruleEngine.getGroups().get(i).getId() + ", failed: " + ruleEngine.getGroups().get(i).getFailedAsString();
   					log.logDetailed(message);
        		}
        	}
        	else if(log.isDebug())
        	{
        		for(int i=0;i<ruleEngine.getGroups().size();i++)
        		{
        			for (int f=0;f<ruleEngine.getGroups().get(i).getSubGroups().size();f++)
        			{
    					String message = "line: " +getLinesRead() + ", group: " + ruleEngine.getGroups().get(i).getId()+ ", failed: " + ruleEngine.getGroups().get(i).getFailedAsString() + ", subgroup: " + ruleEngine.getGroups().get(i).getSubGroups().get(f).getId() + ", failed: " + ruleEngine.getGroups().get(i).getSubGroups().get(f).getFailedAsString();
    					log.logDebug(message);
        			}
        		}
        	}        	
        	else if(log.isRowLevel())
        	{
        		for(int i=0;i<ruleEngine.getGroups().size();i++)
        		{
        			for (int f=0;f<ruleEngine.getGroups().get(i).getSubGroups().size();f++)
        			{
        				for(int g=0;g<ruleEngine.getGroups().get(i).getSubGroups().get(f).getRulesCollection().size();g++)
        				{
        					String message = "line: " +getLinesRead() + ", group: " + ruleEngine.getGroups().get(i).getId() + ", subgroup: " + ruleEngine.getGroups().get(i).getSubGroups().get(f).getId() + ", rule: " + ruleEngine.getGroups().get(i).getSubGroups().get(f).getRulesCollection().get(g).getId() + ", failed: " + ruleEngine.getGroups().get(i).getSubGroups().get(f).getResults().get(g).getFailedAsString() + ", " + ruleEngine.getGroups().get(i).getSubGroups().get(f).getResults().get(g).getMessage();
        					log.logRowlevel(message);
        				}
        			}
        		}
        	}
        }
        catch(Exception ex)
        {
       		log.logError(ex.getMessage());
       		//log.logError(Const.getStackTracker(ex));
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
	           		ValueMetaInterface vmi = inputRowMeta.searchValueMeta(fieldNames[i]);
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
       		//log.logError("error running business rule engine: " + ex.getStackTrace().toString());
       		log.logError("error updating output fields", ex.toString());
       		//log.logError(Const.getStackTracker(ex));
       		setStopped(true);
       		setOutputDone();
       		setErrors(1);
       		stopAll();
        	return false;
        }
        
        // output the rule results 
        try
        {
        	// only if a rule results step is defined and not if we output only
        	// failed groups but there are none
	        if(rowsetRuleResults!= null && !(meta.getOutputType()==1 && ruleEngine.getNumberOfGroupsFailed()==0))
	        {
	        	// loop over all groupserror
	        	log.logDebug("looping all rulegroups");
	        	for(int f=0;f<ruleEngine.getGroups().size();f++)
	            {
	            	RuleGroup group = ruleEngine.getGroups().get(f);
	            	// output groups with all rules depending on the output type selection
		        	if(meta.getOutputType()==0 || (meta.getOutputType()==1 && group.getFailed()==1) || (meta.getOutputType()==2 && group.getFailed()==1) || (meta.getOutputType()==3 && group.getFailed()==0) || (meta.getOutputType()==4 && group.getFailed()==0))
	            	{
		            	// loop over all subgroups
		        		for(int g=0;g<group.getSubGroups().size();g++)
		                {
		            		RuleSubGroup subgroup = group.getSubGroups().get(g);
		            		ArrayList <RuleExecutionResult> results = subgroup.getExecutionCollection().getResults();
		            		// loop over all results
		            		for (int h= 0;h< results.size();h++)
		                    {
		            			RuleExecutionResult result = results.get(h);
		            			XmlRule rule = result.getRule();
		            			// cloning the original row as we will use the same input row for
		            			// multiple output rows. without cloning, there are errors with
		            			// the output rows.
		            			if(meta.getOutputType()==0 || (meta.getOutputType()==1 && rule.getFailed()==1)|| (meta.getOutputType()==2) || (meta.getOutputType()==3 && rule.getFailed()==1) || (meta.getOutputType()==4))
		            			{
			            			Object[] outputRowRuleResultsCloned = outputRowRuleResults.clone();
		            				outputRowRuleResultsCloned[inputSize] = group.getId();
		            				outputRowRuleResultsCloned[inputSize+1] = (long)group.getFailed();
		            				outputRowRuleResultsCloned[inputSize+2] = subgroup.getId();
		            				outputRowRuleResultsCloned[inputSize+3] = (long)subgroup.getFailed();
		            				outputRowRuleResultsCloned[inputSize+4] = subgroup.getLogicalOperatorSubGroupAsString();
		            				outputRowRuleResultsCloned[inputSize+5] = subgroup.getLogicalOperatorRulesAsString();
		            				outputRowRuleResultsCloned[inputSize+6] = rule.getId();
		            				outputRowRuleResultsCloned[inputSize+7] = (long)rule.getFailed();
		            				outputRowRuleResultsCloned[inputSize+8] = result.getMessage();
		            				// put the row to the output step
			                    	putRowTo(data.outputRowMetaRuleResults, outputRowRuleResultsCloned, rowsetRuleResults);
			                    	// set the cloned row to null
			                    	outputRowRuleResultsCloned=null;
		            			}
		                    }
		                }
	            	}
	            }
	        }
        }
        catch(Exception ex)
        {
        	log.logError("error output to rule results step", ex.toString());
        	//log.logError(Const.getStackTracker(ex));
       		setStopped(true);
       		setOutputDone();
       		setErrors(1);
       		stopAll();
        	return false;
        }
        
        // add the generated field values to the main output row
        try
        {
        	log.logDebug("adding ruleengine fields to output row");
	        outputRow[inputSize] = (long)ruleEngine.getNumberOfGroups();
	        outputRow[inputSize +1] = (long)ruleEngine.getNumberOfGroupsFailed();
	        outputRow[inputSize +2] = (long)ruleEngine.getNumberOfRules();
	        outputRow[inputSize +3] = (long)ruleEngine.getNumberOfRules() - (long)ruleEngine.getNumberOfRulesPassed();
	        outputRow[inputSize +4] = (long)ruleEngine.getNumberOfActions();
	        
	        // original line with only one output step
	        // putRow(data.outputRowMeta, outputRow);
	         putRowTo(data.outputRowMeta, outputRow, rowsetMain);
        }
        catch(Exception ex)
        {
       		log.logError("error output to main step", ex.toString());
       		//log.logError(Const.getStackTracker(ex));
       		setStopped(true);
       		setOutputDone();
       		setErrors(1);
       		stopAll();
        	return false;
        }
        
        
        // clear the results for the next run. if this is not done, the results
        // of the rule engine will accumulate
        // but only if the results are not excluded anyway
        if(!ruleEngine.getPreserveRuleExcecutionResults())
        {
        	ruleEngine.getRuleExecutionCollection().clear();
        }
        
		return true;
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (JarePluginMeta)smi;
	    data = (JarePluginData)sdi;

	    return super.init(smi, sdi);
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (JarePluginMeta)smi;
	    data = (JarePluginData)sdi;

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
            //logError(Const.getStackTracker(e));
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
	
	/**
	 * translates a parameter/variable or multiple ones in the form of ${someparam}
	 * into the actual value. if no parameter value  is found, returns
	 * the value that was passed to this method.
	 */
	private String getRealName(String value)
	{
		String pattern = "(\\$\\{.+?\\})";
		String filePattern = "file://";
		
		if(value!= null)
		{
			String returnValue=value;
			Pattern p = Pattern.compile(pattern);
			boolean found= false;
			do
			{
				Matcher matcher = p.matcher(returnValue);
				if (matcher.find()) 
				{
					found=true;
					String parameterName = matcher.group(1).substring(2,matcher.group(1).length()-1);
					String parameterValue = null;
					try
					{
						log.logDebug("trying to retieve parameter: " + parameterName);
						parameterValue=getTrans().getParameterValue(parameterName);
					}
					catch(Exception ex)
					{
						log.logError("error retieving parameter: " + parameterName);
						setStopped(true);
			       		setOutputDone();
			       		setErrors(1);
			       		stopAll();
					}

					if(parameterValue != null)
					{
						if(parameterValue.startsWith(filePattern))
						{
							parameterValue = parameterValue.substring(filePattern.length());
						}
						log.logDebug("parameter found: " + parameterName + " - value: " + parameterValue);
						returnValue = returnValue.replaceFirst(pattern,Matcher.quoteReplacement(parameterValue));
					}
					else
					{
						log.logDebug("parameter not found. trying to retieve variable: " + parameterName);
						parameterValue=getTransMeta().getVariable(parameterName);
						if(parameterValue != null)
						{
							if(parameterValue.startsWith(filePattern))
							{
								parameterValue = parameterValue.substring(filePattern.length());
							}
							returnValue = returnValue.replaceFirst(pattern,Matcher.quoteReplacement(parameterValue));
						}
					}
				}
				else
				{
					found = false;
				}
			} while (found);
			log.logDebug("return value for: " + value + "=" + returnValue);
			return returnValue;
		}
		else
		{
			return value;
		}
	}
	
	private void addFieldstoRowMeta(RowMetaInterface r, String origin, boolean ruleResults)
	{
		if(ruleResults)
		{
			//ValueMetaInterface group = new ValueMeta("ruleengine_group", ValueMeta.TYPE_STRING);
			ValueMetaInterface group = new ValueMetaString("ruleengine_group");
			group.setOrigin(origin);
			r.addValueMeta( group );
			
			//ValueMetaInterface groupFailed = new ValueMeta("ruleengine_group_failed", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface groupFailed = new ValueMetaInteger("ruleengine_group_failed");
			groupFailed.setOrigin(origin);
			r.addValueMeta( groupFailed );
			
			//ValueMetaInterface subgroup = new ValueMeta("ruleengine_subgroup", ValueMeta.TYPE_STRING);
			ValueMetaInterface subgroup = new ValueMetaString("ruleengine_subgroup");
			subgroup.setOrigin(origin);
			r.addValueMeta( subgroup );

			//ValueMetaInterface subgroupFailed = new ValueMeta("ruleengine_subgroup_failed", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface subgroupFailed = new ValueMetaInteger("ruleengine_subgroup_failed");
			subgroupFailed.setOrigin(origin);
			r.addValueMeta( subgroupFailed );

			//ValueMetaInterface subgroupIntergroupOperator = new ValueMeta("ruleengine_subgroup_intergroup_operator", ValueMeta.TYPE_STRING);
			ValueMetaInterface subgroupIntergroupOperator = new ValueMetaString("ruleengine_subgroup_intergroup_operator");
			subgroupIntergroupOperator.setOrigin(origin);
			r.addValueMeta( subgroupIntergroupOperator );

			//ValueMetaInterface subgroupRuleOperator = new ValueMeta("ruleengine_subgroup_rule_operator", ValueMeta.TYPE_STRING);
			ValueMetaInterface subgroupRuleOperator = new ValueMetaString("ruleengine_subgroup_rule_operator");
			subgroupRuleOperator.setOrigin(origin);
			r.addValueMeta( subgroupRuleOperator );
			
			//ValueMetaInterface rule = new ValueMeta("ruleengine_rule", ValueMeta.TYPE_STRING);
			ValueMetaInterface rule = new ValueMetaString("ruleengine_rule");
			rule.setOrigin(origin);
			r.addValueMeta( rule );
			
			//ValueMetaInterface ruleFailed = new ValueMeta("ruleengine_rule_failed", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface ruleFailed = new ValueMetaInteger("ruleengine_rule_failed");
			ruleFailed.setOrigin(origin);
			r.addValueMeta( ruleFailed );
			
			//ValueMetaInterface ruleMessage = new ValueMeta("ruleengine_message", ValueMeta.TYPE_STRING);
			ValueMetaInterface ruleMessage = new ValueMetaString("ruleengine_message");
			ruleMessage.setOrigin(origin);
			r.addValueMeta( ruleMessage );
		}
		else if(!ruleResults)
		{
			//ValueMetaInterface totalGroups=new ValueMeta("ruleengine_groups", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface totalGroups=new ValueMetaInteger("ruleengine_groups");
			totalGroups.setOrigin(origin);
			r.addValueMeta( totalGroups );
			
			//ValueMetaInterface totalGroupsFailed=new ValueMeta("ruleengine_groups_failed", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface totalGroupsFailed=new ValueMetaInteger("ruleengine_groups_failed");
			totalGroupsFailed.setOrigin(origin);
			r.addValueMeta( totalGroupsFailed );
			
			//ValueMetaInterface totalRules=new ValueMeta("ruleengine_rules", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface totalRules=new ValueMetaInteger("ruleengine_rules");
			totalRules.setOrigin(origin);
			r.addValueMeta( totalRules );
			
			//ValueMetaInterface totalRulesFailed=new ValueMeta("ruleengine_rules_failed", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface totalRulesFailed=new ValueMetaInteger("ruleengine_rules_failed");
			totalRulesFailed.setOrigin(origin);
			r.addValueMeta( totalRulesFailed );
			
			//ValueMetaInterface totalActions=new ValueMeta("ruleengine_actions", ValueMeta.TYPE_INTEGER);
			ValueMetaInterface totalActions=new ValueMetaInteger("ruleengine_actions");
			totalActions.setOrigin(origin);
			r.addValueMeta( totalActions );
		}
	}
}
