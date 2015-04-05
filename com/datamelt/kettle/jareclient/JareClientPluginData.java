package com.datamelt.kettle.jareclient;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * 
 * 
 */
public class JareClientPluginData extends BaseStepData implements StepDataInterface
{
	public RowMetaInterface outputRowMeta;

    public JareClientPluginData()
	{
		super();
	}
}
