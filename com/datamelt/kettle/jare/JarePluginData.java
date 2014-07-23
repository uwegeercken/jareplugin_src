package com.datamelt.kettle.jare;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * 
 * 
 */
public class JarePluginData extends BaseStepData implements StepDataInterface
{
	public RowMetaInterface outputRowMeta;
	public RowMetaInterface outputRowMetaRuleResults;

    public JarePluginData()
	{
		super();
	}
}
