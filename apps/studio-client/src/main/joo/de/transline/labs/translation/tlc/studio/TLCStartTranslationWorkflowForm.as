package de.transline.labs.translation.tlc.studio {
import com.coremedia.cms.editor.controlroom.workflow.translation.DefaultStartTranslationWorkflowForm;

public class TLCStartTranslationWorkflowForm extends DefaultStartTranslationWorkflowForm{


  public function TLCStartTranslationWorkflowForm(config:DefaultStartTranslationWorkflowForm = null) {
    super(config);
  }

  override public function isPullTranslation():Boolean {
    //for this workflow a PullTranslation should be disabled
    return false;
  }
}
}
