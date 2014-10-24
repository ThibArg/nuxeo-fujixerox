/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     thibaud
 */

package org.nuxeo.fujixerox;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * This listener is called for "About t Create" and "Before modification". Nuxeo
 * Studio project is not involved here
 */
public class ValidatePictureMetadataListener implements EventListener {

    // private static final Log log =
    // LogFactory.getLog(ValidatePictureMetadataListener.class);

    @Override
    public void handleEvent(Event event) throws ClientException {

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel doc = context.getSourceDocument();
            if (!doc.isImmutable() && doc.getType().equals("Picture")) {

                // log.warn(event.getName());

                // We check the binary only if
                // -> We are in the "before creation" event
                // -> or in "before modification" *and* the blob is dirty
                Property blobProp = doc.getProperty("file:content");
                if (DocumentEventTypes.ABOUT_TO_CREATE.equals(event.getName())
                        || (DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName()) && blobProp.isDirty())) {

                    Blob theBlob = (Blob) blobProp.getValue();
                    if (theBlob != null) {
                        String errorMsg = "";
                        errorMsg = ValidatePictureMetadata.validate(theBlob);
                        if (!errorMsg.isEmpty()) {
                            // Rollback the transaction and return the error
                            event.markRollBack(errorMsg,
                                    new RecoverableClientException(errorMsg,
                                            errorMsg, null));
                        }
                    }
                }
            }
        }
    }
}
