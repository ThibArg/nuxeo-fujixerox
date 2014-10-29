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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.picture.PictureViewsGenerationWork;
import org.nuxeo.fujixerox.storedpicturerendition.StoredPictureRenditionBuilder;

/*
 * We listen to the "pictureViewsGenerationDone" event, which is an asynchronous
 * listener triggered once the default work has been done by nuxeo: Thumbnail,
 * creation of views (small, medium, normal)
 *
 * This is "pictureViewsGenerationDone" is also very interesting because it is
 * triggered only if the binary has been modified (new document or change binary
 * on existing document), so we know that if we are here, the binary must be
 * handled, which means, store them in the picture:views schema, with the name
 * of each rendition being the key.
 *
 * ===========================================================================
 * IMPORTANT
 * ===========================================================================
 * We are in a postCommit event => we must implements PostCommitEventListener,
 * not EventListener
 *
 */
public class PictureViewsGenerationDoneListener implements
        PostCommitEventListener {

    static private Log log = LogFactory.getLog(PictureViewsGenerationDoneListener.class);

    protected static final String PICTURE_VIEWS_GENERATION_DONE_EVENTNAME = PictureViewsGenerationWork.PICTURE_VIEWS_GENERATION_DONE_EVENT;

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {
        for (Event oneEvent : bundle) {
            // Usual sanity check
            if (!PICTURE_VIEWS_GENERATION_DONE_EVENTNAME.equals(oneEvent.getName())) {
                log.warn("Receiving the <"
                        + oneEvent.getName()
                        + "> event, while the XML definition declares we listen only to "
                        + PICTURE_VIEWS_GENERATION_DONE_EVENTNAME
                        + " => ignoring this event");
                continue;
            }
            EventContext ctx = oneEvent.getContext();
            if (!(ctx instanceof DocumentEventContext)) {
                log.warn("The event context should be a DocumentEventContext => ignoring the event");
                continue;
            }

            DocumentEventContext docCtx = (DocumentEventContext) oneEvent.getContext();
            DocumentModel doc = docCtx.getSourceDocument();

            if (doc.isImmutable()) {
                // log.warn("The doc ID " + doc.getId() +
                // " is immutable => ignoring the event");
                continue;
            }

            if (!doc.hasSchema("picture")) {
                log.warn("Strange. Being called for <"
                        + PICTURE_VIEWS_GENERATION_DONE_EVENTNAME
                        + "> event while the current doc (" + doc.getId()
                        + ") has no 'picture' schema => ignoring the event");
            }

            StoredPictureRenditionBuilder rh = new StoredPictureRenditionBuilder(
                    doc);
            try {

                rh.buildAvailableRenditions();

                // ======================================================================
                // If the pattern is to set a flag on a field of the document,
                // this is where it must be done
                // doc.setPropertyValue("schemaprefix:fieldname", true);
                // ======================================================================

                // The doc will be saved only if no error occurred (else, in
                // case of error, we'll be in the catch() part)
                doc = doc.getCoreSession().saveDocument(doc);

                // ======================================================================
                // If the pattern is to send a push notification to a webservice
                // and/or to send an email, this is where it must be done
                // ======================================================================

            } catch (CommandNotAvailable | IOException | CommandException e) {
                throw new ClientException(
                        "Failed to pre-build the renditions for document "
                                + doc.getId(), e);
            }
        }
    }
}
