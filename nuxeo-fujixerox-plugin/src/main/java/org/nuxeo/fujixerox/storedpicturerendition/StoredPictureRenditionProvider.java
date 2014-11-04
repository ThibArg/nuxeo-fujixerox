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
 *     thibaud
 */
package org.nuxeo.fujixerox.storedpicturerendition;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * The main principle of this provider is that each rendition is stored in the
 * picture:views field. This way, it is easy to add a rendition or get a
 * rendition thanks to {@link MultiviewPicture}.
 * <p>
 * Storage is done by {@link StoredPictureRenditionBuilder}
 * <p>
 * <b>IMPORTANT</b>: This provider does not calculate the rendition if it does not
 * exist, it <b>assumes all renditions have already been stored</b>
 * <p>
 *
 * @since 5.9.5
 */
public class StoredPictureRenditionProvider implements RenditionProvider {

    // static private Log log =
    // LogFactory.getLog(StoredPictureRenditionProvider.class);

    /*
     * We will be called only for the renditions we defined. More precisely,
     * only for the renditions declared in XMLs which reference this class.
     */
    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition definition) {

        // We handle only "Picture" document
        if (!doc.getType().equals("Picture")) {
            return false;
        }

        // Check we have something in picture:views, trying to avoid returning
        // null when render() is called.
        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
        if (mvp != null) {
            return mvp.getView(definition.getName()) != null;
        }
        return false;
    }

    /*
     * The name of the rendition is the key in the picture:views field.
     */
    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition)
            throws RenditionException {

        List<Blob> blobs = new ArrayList<Blob>();

        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
        if (mvp != null) {
            PictureView pv = mvp.getView(definition.getName());
            if (pv != null) {
                blobs.add(pv.getBlob());
            }
        }

        return blobs;
    }

}
