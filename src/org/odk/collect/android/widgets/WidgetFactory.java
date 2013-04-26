/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.widgets;

import java.util.Locale;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.util.Log;

/**
 * Convenience class that handles creation of widgets.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class WidgetFactory {

    /**
     * Returns the appropriate QuestionWidget for the given FormEntryPrompt.
     *
     * @param fep prompt element to be rendered
     * @param context Android context
     */
    static public QuestionWidget createWidgetFromPrompt(FormEntryPrompt fep, Activity activity) {

    	// get appearance hint and clean it up so it is lower case and never null...
        String appearance = fep.getAppearanceHint();
        if ( appearance == null ) appearance = "";
        // for now, all appearance tags are in english...
        appearance = appearance.toLowerCase(Locale.ENGLISH);

        QuestionWidget questionWidget = null;
        switch (fep.getControlType()) {
            case Constants.CONTROL_INPUT:
                switch (fep.getDataType()) {
                    case Constants.DATATYPE_DATE_TIME:
                        questionWidget = new DateTimeWidget(activity, fep);
                        break;
                    case Constants.DATATYPE_DATE:
                        questionWidget = new DateWidget(activity, fep);
                        break;
                    case Constants.DATATYPE_TIME:
                        questionWidget = new TimeWidget(activity, fep);
                        break;
                    case Constants.DATATYPE_DECIMAL:
                    	if ( appearance.startsWith("ex:") ) {
                    		questionWidget = new ExDecimalWidget(activity, fep);
                    	} else {
                    		questionWidget = new DecimalWidget(activity, fep);
                    	}
                        break;
                    case Constants.DATATYPE_INTEGER:
                    	if ( appearance.startsWith("ex:") ) {
                    		questionWidget = new ExIntegerWidget(activity, fep);
                    	} else {
                    		questionWidget = new IntegerWidget(activity, fep);
                    	}
                        break;
                    case Constants.DATATYPE_GEOPOINT:
                        questionWidget = new GeoPointWidget(activity, fep);
                        break;
                    case Constants.DATATYPE_BARCODE:
                        questionWidget = new BarcodeWidget(activity, fep);
                        break;
                    case Constants.DATATYPE_TEXT:
                    	if (appearance.startsWith("printer")) {
                            questionWidget = new ExPrinterWidget(activity, fep);
                    	} else if (appearance.startsWith("ex:")) {
                            questionWidget = new ExStringWidget(activity, fep);
                    	} else if (appearance.equals("numbers")) {
                            questionWidget = new StringNumberWidget(activity, fep);
                        } else {
                            questionWidget = new StringWidget(activity, fep);
                        }
                        break;
                    default:
                        questionWidget = new StringWidget(activity, fep);
                        break;
                }
                break;
            case Constants.CONTROL_IMAGE_CHOOSE:
            	if (appearance.equals("web")) {
            		questionWidget = new ImageWebViewWidget(activity, fep);
        		} else if(appearance.equals("signature")) {
            		questionWidget = new SignatureWidget(activity, fep);
            	} else if(appearance.equals("annotate")) {
            		questionWidget = new AnnotateWidget(activity, fep);
            	} else if(appearance.equals("draw")) {
            		questionWidget = new DrawWidget(activity, fep);
            	} else {
            		questionWidget = new ImageWidget(activity, fep);
            	}
                break;
            case Constants.CONTROL_AUDIO_CAPTURE:
                questionWidget = new AudioWidget(activity, fep);
                break;
            case Constants.CONTROL_VIDEO_CAPTURE:
                questionWidget = new VideoWidget(activity, fep);
                break;
            case Constants.CONTROL_SELECT_ONE:
                if (appearance.contains("compact")) {
                    int numColumns = -1;
                    try {
                    	int idx = appearance.indexOf("-");
                    	if ( idx != -1 ) {
                    		numColumns =
                    				Integer.parseInt(appearance.substring(idx + 1));
                    	}
                    } catch (Exception e) {
                        // Do nothing, leave numColumns as -1
                        Log.e("WidgetFactory", "Exception parsing numColumns");
                    }

                    if (appearance.contains("quick")) {
                        questionWidget = new GridWidget(activity, fep, numColumns, true);
                    } else {
                        questionWidget = new GridWidget(activity, fep, numColumns, false);
                    }
                } else if (appearance.equals("minimal")) {
                    questionWidget = new SpinnerWidget(activity, fep);
                }
                // else if (appearance != null && appearance.contains("autocomplete")) {
                // String filterType = null;
                // try {
                // filterType = appearance.substring(appearance.indexOf('-') + 1);
                // } catch (Exception e) {
                // // Do nothing, leave filerType null
                // Log.e("WidgetFactory", "Exception parsing filterType");
                // }
                // questionWidget = new AutoCompleteWidget(context, fep, filterType);
                //
                // }
                else if (appearance.equals("quick")) {
                    questionWidget = new SelectOneAutoAdvanceWidget(activity, fep);
                } else if (appearance.equals("list")) {
                    questionWidget = new ListWidget(activity, fep, true);
                } else if (appearance.equals("list-nolabel")) {
                    questionWidget = new ListWidget(activity, fep, false);
                } else if (appearance.equals("label")) {
                    questionWidget = new LabelWidget(activity, fep);
                } else {
                    questionWidget = new SelectOneWidget(activity, fep);
                }
                break;
            case Constants.CONTROL_SELECT_MULTI:
                if (appearance.contains("compact")) {
                    int numColumns = -1;
                    try {
                    	int idx = appearance.indexOf("-");
                    	if ( idx != -1 ) {
                    		numColumns =
                    				Integer.parseInt(appearance.substring(idx + 1));
                    	}
                    } catch (Exception e) {
                        // Do nothing, leave numColumns as -1
                        Log.e("WidgetFactory", "Exception parsing numColumns");
                    }

                    questionWidget = new GridMultiWidget(activity, fep, numColumns);
                } else if (appearance.equals("minimal")) {
                    questionWidget = new SpinnerMultiWidget(activity, fep);
                } else if (appearance.equals("list")) {
                    questionWidget = new ListMultiWidget(activity, fep, true);
                } else if (appearance.equals("list-nolabel")) {
                    questionWidget = new ListMultiWidget(activity, fep, false);
                } else if (appearance.equals("label")) {
                    questionWidget = new LabelWidget(activity, fep);
                } else {
                    questionWidget = new SelectMultiWidget(activity, fep);
                }
                break;
            case Constants.CONTROL_TRIGGER:
                questionWidget = new TriggerWidget(activity, fep);
                break;
            default:
                questionWidget = new StringWidget(activity, fep);
                break;
        }
        return questionWidget;
    }

}
