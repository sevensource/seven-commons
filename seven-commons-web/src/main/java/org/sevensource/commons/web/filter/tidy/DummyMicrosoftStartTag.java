package org.sevensource.commons.web.filter.tidy;
import net.htmlparser.jericho.StartTagTypeGenericImplementation;
 
/**
* The Jericho implementation recognises the last tag in the following example as the beginning of a comment<br>
* <!--[if (gt IEMobile 7)|!(IEMobile)]><!-->
*
 * @author philipp.gaschuetz
*
*/
final class DummyMicrosoftStartTag extends StartTagTypeGenericImplementation {
       static final DummyMicrosoftStartTag INSTANCE = new DummyMicrosoftStartTag();
 
       private DummyMicrosoftStartTag() {
             super("DummyMicrosoftStartTag","<!-->","",null,false);
       }
}