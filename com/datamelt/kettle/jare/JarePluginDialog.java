package com.datamelt.kettle.jare;

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
//import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;

public class JarePluginDialog extends BaseStepDialog implements StepDialogInterface
{
	private JarePluginMeta input;

	private Label        wLabelRuleFile, wLabelStepname, wLabelOutputType,wLabelStepMain, wLabelStepRuleResults;
	private Text         wTextRuleFile, wTextStepname;
	private Combo		 wComboOutputType, wComboStepRuleResults, wComboStepMain;
	private FormData     wFormRuleFile, wFormStepname, wFormOutputType, wFormStepMain, wFormStepRuleResults;

	public JarePluginDialog(Shell parent, Object in, TransMeta transMeta, String sname)
	{
		super(parent, (BaseStepMeta)in, transMeta, sname);
		input=(JarePluginMeta)in;
		
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
		
		// Rule File Name line
		wLabelRuleFile=new Label(shell, SWT.RIGHT);
		wLabelRuleFile.setText(Messages.getString("JarePluginDialog.RuleFile.Label")); //$NON-NLS-1$
        props.setLook( wLabelRuleFile );
        wFormRuleFile=new FormData();
        wFormRuleFile.left = new FormAttachment(0, 0);
        wFormRuleFile.right= new FormAttachment(middle, -margin);
        wFormRuleFile.top  = new FormAttachment(wTextStepname, margin);
		wLabelRuleFile.setLayoutData(wFormRuleFile);
		wTextRuleFile=new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
		if(input.getRuleFileName()!=null)
		{
			wTextRuleFile.setText(input.getRuleFileName());
		}
        props.setLook( wTextRuleFile );
        wTextRuleFile.addModifyListener(lsMod);
		wFormRuleFile=new FormData();
		wFormRuleFile.left = new FormAttachment(middle, 0);
		wFormRuleFile.top  = new FormAttachment(wTextStepname, margin);
		wFormRuleFile.right= new FormAttachment(100, 0);
		wTextRuleFile.setLayoutData(wFormRuleFile);
		
		// Main Output Step
		wLabelStepMain=new Label(shell, SWT.RIGHT);
		wLabelStepMain.setText(Messages.getString("JarePluginDialog.Step.Main")); //$NON-NLS-1$
        props.setLook( wLabelStepMain );
        wFormStepMain=new FormData();
        wFormStepMain.left = new FormAttachment(0, 0);
        wFormStepMain.right= new FormAttachment(middle, -margin);
        wFormStepMain.top  = new FormAttachment(wTextRuleFile, margin);
        wLabelStepMain.setLayoutData(wFormStepMain);
		wComboStepMain=new Combo(shell, SWT.VERTICAL | SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		String outputSteps[] = transMeta.getNextStepNames(stepMeta);
		for(int i=0;i<outputSteps.length;i++)
		{
			wComboStepMain.add(outputSteps[i]);
		}
		if(input.getStepMain()!=null)
		{
			wComboStepMain.setText(input.getStepMain());
		}
		props.setLook( wComboStepMain );
		wComboStepMain.addModifyListener(lsMod);
		wFormStepMain=new FormData();
		wFormStepMain.left = new FormAttachment(middle, 0);
		wFormStepMain.top  = new FormAttachment(wTextRuleFile, margin);
		wFormStepMain.right= new FormAttachment(100, 0);
        wComboStepMain.setLayoutData(wFormStepMain);
		
		// Rule Results Output Step
		wLabelStepRuleResults=new Label(shell, SWT.RIGHT);
		wLabelStepRuleResults.setText(Messages.getString("JarePluginDialog.Step.RuleResults")); //$NON-NLS-1$
        props.setLook( wLabelStepRuleResults );
        wFormStepRuleResults=new FormData();
        wFormStepRuleResults.left = new FormAttachment(0, 0);
        wFormStepRuleResults.right= new FormAttachment(middle, -margin);
        wFormStepRuleResults.top  = new FormAttachment(wComboStepMain, margin);
        wLabelStepRuleResults.setLayoutData(wFormStepRuleResults);
		wComboStepRuleResults=new Combo(shell, SWT.VERTICAL | SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		wComboStepRuleResults.add(Messages.getString("JarePluginDialog.Step.RuleResults.Type"));
		for(int i=0;i<outputSteps.length;i++)
		{
			wComboStepRuleResults.add(outputSteps[i]);
		}
		if(input.getStepRuleResults()!=null)
		{
			wComboStepRuleResults.setText(input.getStepRuleResults());
		}
		else
		{
			wComboStepRuleResults.select(0);
		}
		props.setLook( wComboStepRuleResults );
		wComboStepRuleResults.addModifyListener(lsMod);
        wFormStepRuleResults=new FormData();
        wFormStepRuleResults.left = new FormAttachment(middle, 0);
        wFormStepRuleResults.top  = new FormAttachment(wComboStepMain, margin);
        wFormStepRuleResults.right= new FormAttachment(100, 0);
        wComboStepRuleResults.setLayoutData(wFormStepRuleResults);
		
		// Rule Results Output Step Output Type
		wLabelOutputType=new Label(shell, SWT.RIGHT);
		wLabelOutputType.setText(Messages.getString("JarePluginDialog.OutputType.Label")); //$NON-NLS-1$
        props.setLook( wLabelOutputType );
        wFormOutputType=new FormData();
        wFormOutputType.left = new FormAttachment(0, 0);
        wFormOutputType.right= new FormAttachment(middle, -margin);
        wFormOutputType.top  = new FormAttachment(wComboStepRuleResults, margin);
		wLabelOutputType.setLayoutData(wFormOutputType);
		wComboOutputType=new Combo(shell, SWT.VERTICAL | SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		wComboOutputType.add(Messages.getString("JarePluginDialog.OutputType.ComboEntry_0"));
		wComboOutputType.add(Messages.getString("JarePluginDialog.OutputType.ComboEntry_1"));
		wComboOutputType.add(Messages.getString("JarePluginDialog.OutputType.ComboEntry_2"));
		wComboOutputType.add(Messages.getString("JarePluginDialog.OutputType.ComboEntry_3"));
		wComboOutputType.add(Messages.getString("JarePluginDialog.OutputType.ComboEntry_4"));
		wComboOutputType.select(input.getOutputType());
		props.setLook( wComboOutputType );
        wComboOutputType.addModifyListener(lsMod);
        wFormOutputType=new FormData();
        wFormOutputType.left = new FormAttachment(middle, 0);
        wFormOutputType.top  = new FormAttachment(wComboStepRuleResults, margin);
        wFormOutputType.right= new FormAttachment(100, 0);
		wComboOutputType.setLayoutData(wFormOutputType);
	
		// Some buttons
		wOK=new Button(shell, SWT.PUSH);
		wOK.setText(Messages.getString("System.Button.OK")); //$NON-NLS-1$
		wCancel=new Button(shell, SWT.PUSH);
		wCancel.setText(Messages.getString("System.Button.Cancel")); //$NON-NLS-1$

        BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel}, margin, wComboOutputType);
        
		// Add listeners
		lsCancel   = new Listener() { public void handleEvent(Event e) { cancel(); } };
		lsOK       = new Listener() { public void handleEvent(Event e) { ok();     } };
		
		wCancel.addListener(SWT.Selection, lsCancel);
		wOK.addListener    (SWT.Selection, lsOK    );
		
		lsDef=new SelectionAdapter() { public void widgetDefaultSelected(SelectionEvent e) { ok(); } };
		
		wTextStepname.addSelectionListener( lsDef );
		wTextRuleFile.addSelectionListener( lsDef );
		wComboOutputType.addSelectionListener( lsDef );
		wComboStepMain.addSelectionListener( lsDef );
		wComboStepRuleResults.addSelectionListener( lsDef );
		
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
		input.setRuleFileName(wTextRuleFile.getText());
		input.setStepMain(wComboStepMain.getText());
		input.setStepRuleResults(wComboStepRuleResults.getText());
		input.setOutputType(wComboOutputType.getSelectionIndex());
		
		dispose();
	}
}
