/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.fujixerox;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;
import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.UPDATE_PICTURE_VIEW_EVENT;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.picture.listener.PictureChangedListener;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * IMPORTANT: This listener overrides the default one, to work around bug
 * NXP-15836 NOTICE: If it is removed, think about also removing the
 * PictureChangedCustomListener.xml contribution and the MANIFEST.MF, which
 * references this contribution
 *
 * Also, because we are using the picture:views schema, we had another problem:
 * nuxeo makes some assumption about this schema, and does not always enters the
 * code that fills the views and, then, ultimately, sends the
 * "pictureViewsGenerationDone", while main parts of the logic of the
 * nuxeo-fujixerox plug-in is working in this event. So we override the default
 * behavior at the cost of re-calculating the default picture:views. even if,
 * possibly, they are already ok.
 *
 * =>Final plug-in should optimize this.
 *
 * If there are other metadata and custom schemas, maybe one could just handle
 * renditions in this custom schema, in an asynchronous/postcommit
 * "document created/modifed" event, so there will be no more dependencies on
 * picture:views.
 */
public class PictureChangedListenerCustom extends PictureChangedListener {

    private static Log log = LogFactory.getLog(PictureChangedListenerCustom.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();

        if (!doc.isImmutable()) {
            // super.handleEvent(event);
            if (doc.hasFacet(PICTURE_FACET) && !doc.isProxy()) {
                Property fileProp = doc.getProperty("file:content");
                if (DOCUMENT_CREATED.equals(event.getName())
                        || fileProp.isDirty()) {
                    // Here is the difference with the original
                    // PictureChangedListener: We always
                    // calculate the views, so pictureViewsGenerationDone
                    // event is fired by nuxeo.
                    preFillPictureViews(docCtx.getCoreSession(), doc);
                    // mark the document as needing picture views generation
                    Event trigger = docCtx.newEvent(UPDATE_PICTURE_VIEW_EVENT);
                    EventService eventService = Framework.getLocalService(EventService.class);
                    eventService.fireEvent(trigger);
                }
            }
        }
    }
}
