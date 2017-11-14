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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;

public class JareClientPluginDialog extends BaseStepDialog implements StepDialogInterface
{
	private JareClientPluginMeta input;

	private Label        wLabelServer, wLabelStepname, wLabelServerPort;
	private Text         wTextStepname;
	private TextVar      wTextServer, wTextServerPort;
	private FormData     wFormServer, wFormStepname, wFormServerPort;
	

	public JareClientPluginDialog(Shell parent, Object in, TransMeta transMeta, String sname)
	{
		super(parent, (BaseStepMeta)in, transMeta, sname);
		input=(JareClientPluginMeta)in;

	}

	public String open()
	{
		Shell parent = getParent();
		Display display = parent.getDisplay();
		
		shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
		props.setLook( shell );
        setShellImage(shell, input);

       
        
        
		ModifyListener lsMod = new ModifyListener() 
		{
			public void modifyText(ModifyEvent e) 
			{
				input.setChanged();
			}
		};
		changed = input.hasChanged();
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth  = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		shell.setLayout(formLayout);
		shell.setText(Messages.getString("JarePluginDialog.Shell.Title")); //$NON-NLS-1$
		
		int middle = props.getMiddlePct();
		int margin = Const.MARGIN;

		// Stepname line
		wLabelStepname=new Label(shell, SWT.RIGHT);
		wLabelStepname.setText(Messages.getString("JarePluginDialog.StepName.Label")); //$NON-NLS-1$
        props.setLook( wLabelStepname );
        wFormStepname=new FormData();
        wFormStepname.left = new FormAttachment(0, 0);
        wFormStepname.right= new FormAttachment(middle, -margin);
        wFormStepname.top  = new FormAttachment(0, margin);
		wLabelStepname.setLayoutData(wFormStepname);
		wTextStepname=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wTextStepname.setText(stepname);
        props.setLook( wTextStepname );
        wTextStepname.addModifyListener(lsMod);
		wFormStepname=new FormData();
		wFormStepname.left = new FormAttachment(middle, 0);
		wFormStepname.top  = new FormAttachment(0, margin);
		wFormStepname.right= new FormAttachment(100, 0);
		wTextStepname.setLayoutData(wFormStepname);
		
		// Server name/ip
		wLabelServer=new Label(shell, SWT.RIGHT);
		wLabelServer.setText(Messages.getString("JarePluginDialog.Server.Label")); //$NON-NLS-1$
        props.setLook( wLabelServer );
        wFormServer=new FormData();
        wFormServer.left = new FormAttachment(0, 0);
        wFormServer.right= new FormAttachment(middle, -margin);
        wFormServer.top  = new FormAttachment(wTextStepname, margin);
		wLabelServer.setLayoutData(wFormServer);
		//wTextServer=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		wTextServer = new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
		if(input.getServer()!=null)
		{
			wTextServer.setText(input.getServer());
		}
        props.setLook( wTextServer );
        wTextServer.addModifyListener(lsMod);
		wFormServer=new FormData();
		wFormServer.left = new FormAttachment(middle, 0);
		wFormServer.top  = new FormAttachment(wTextStepname, margin);
		wFormServer.right= new FormAttachment(100, 0);
		wTextServer.setLayoutData(wFormServer);
		
		// Server port line
		wLabelServerPort=new Label(shell, SWT.RIGHT);
		wLabelServerPort.setText(Messages.getString("JarePluginDialog.ServerPort.Label")); //$NON-NLS-1$
        props.setLook( wLabelServerPort );
        wFormServerPort=new FormData();
        wFormServerPort.left = new FormAttachment(0, 0);
        wFormServerPort.right= new FormAttachment(middle, -margin);
        wFormServerPort.top  = new FormAttachment(wTextServer, margin);
		wLabelServerPort.setLayoutData(wFormServerPort);
		wTextServerPort=new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
		if(input.getServerPort()!=null)
		{
			wTextServerPort.setText(input.getServerPort());
		}
        props.setLook( wTextServerPort );
        wTextServerPort.addModifyListener(lsMod);
		wFormServerPort=new FormData();
		wFormServerPort.left = new FormAttachment(middle, 0);
		wFormServerPort.top  = new FormAttachment(wTextServer, margin);
		wFormServerPort.right= new FormAttachment(100, 0);
		wTextServerPort.setLayoutData(wFormServerPort);
		
		// Some buttons
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK")); //$NON-NLS-1$
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel")); //$NON-NLS-1$

        BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel}, margin, wTextServerPort);
        
		// Add listeners
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
		
		wCancel.addListener(SWT.Selection, lsCancel);
		wOK.addListener    (SWT.Selection, lsOK    );
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wTextStepname.addSelectionListener( lsDef );
		wTextServer.addSelectionListener( lsDef );
		wTextServerPort.addSelectionListener( lsDef );
		
		// Detect X or ALT-F4 or something that kills this window...
		shell.addShellListener(	new ShellAdapter() { public void shellClosed(ShellEvent e) { cancel(); } } );

		// Set the shell size, based upon previous time...
		setSize();
		
		getData();
		input.setChanged(changed);
	
		shell.open();
		
		while (!shell.isDisposed())
		{
		    if (!display.readAndDispatch()) display.sleep();
		}
		return stepname;
	}
	
	// Read data from input (TextFileInputInfo)
	public void getData()
	{
		//wTextRuleFile.setText(input.getRuleFileName());

	}
	
	private void cancel()
	{
		stepname=null;
		input.setChanged(changed);
		dispose();
	}
	
	private void ok()
	{
		stepname = wTextStepname.getText();
		input.setServer(wTextServer.getText());
		input.setServerPort(wTextServerPort.getText());
	
		dispose();
	}
}
