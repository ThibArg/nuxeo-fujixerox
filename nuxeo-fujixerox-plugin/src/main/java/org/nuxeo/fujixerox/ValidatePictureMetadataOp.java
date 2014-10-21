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

import java.util.ArrayList;
import java.util.HashMap;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.imagemetadata.ImageMetadataReader;
import org.nuxeo.imagemetadata.XYResolutionDPI;
import org.nuxeo.imagemetadata.ImageMetadataConstants.KEYS;

/**
 * * * * * * * * * * * * * * IMPORTANT * * * * * * * * * * * * * * * * * * * * *
 *
 * This plug-in uses the ImageMetadataReader class from
 * nuxeo-imagemetadata-utils plug-in. You must add the source.jar to your
 * project: Properties of your project > Java Build Path > Libraries >
 * >"Add External .jar"
 *
 * As of today (2014-10-21) we can't just add nuxeo-imagemetadata-utils as a
 * dependency in the .pom, because it is not available in public repository. it
 * is a project hosted on https://github.com/ThibArg/nuxeo-imagemetadata-utils
 * This is why you need to add this dependency to your Eclipse *and* make sure
 * nuxeo-imagemetadata-utils is installed on your server
 *
 * Also, this image-imagemetadata-utils plug-in *must* be installed on your
 * server, because it also install the im4java library, etc.
 *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */
@Operation(id = ValidatePictureMetadataOp.ID, category = Constants.CAT_DOCUMENT, label = "Validate Picture Metadata", description = "This operation check if the <code>file:content</code> binary has <code>x/y resolution</code> and <code>colorspace</code> set. If not, it throws an exception.")
public class ValidatePictureMetadataOp {

    public static final String ID = "ValidatePictureMetadataOp";

    // private static final Log log =
    // LogFactory.getLog(ValidatePictureMetadataOp.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService automationService;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws ClientException,
            InvalidChainException, OperationException, Exception {

        // Sanity check
        if (inDoc.isImmutable()) {
            throw new ClientException(
                    "The document cannot be a version or a proxy");
        }
        if (!inDoc.hasSchema("picture")) {
            throw new ClientException(
                    "The document does not have the 'picture' schema");
        }
        if (!inDoc.hasSchema("image_metadata")) {
            throw new ClientException(
                    "The document does not have the 'image_metadata' schema");
        }

        // Get the blob
        Blob theBlob = null;
        try {
            theBlob = (Blob) inDoc.getPropertyValue("file:content");
        } catch (PropertyException e) {
            theBlob = null;
        }
        if (theBlob == null) {
            throw new ClientException("The document has no binary attached");
        }

        // Read the Metadata
        ImageMetadataReader imdr = new ImageMetadataReader(theBlob);
        HashMap<String, String> result = null;
        // ==================================================
        // . . .Here, you could adapt and add more business rules about metadata
        // validation. . .
        // ==================================================
        String[] keysStr = { KEYS.COLORSPACE, KEYS.RESOLUTION, KEYS.UNITS };
        result = imdr.getMetadata(keysStr);
        // Resolution needs extra work
        XYResolutionDPI dpi = new XYResolutionDPI(result.get(KEYS.RESOLUTION),
                result.get(KEYS.UNITS));

        // ==================================================
        // Check values
        // ==================================================
        // Trying to add refinement and details to the error message :-)
        ArrayList<String> errors = new ArrayList<String>();
        if (dpi.getX() == 0) {
            errors.add("X-Resolution");
        }
        if (dpi.getY() == 0) {
            errors.add("Y-Resolution");
        }
        if (result.get(KEYS.COLORSPACE).isEmpty()) {
            errors.add("Colorspace");
        }
        // . . . add your other business rules here . . .

        int count = errors.size();
        if (count > 0) {
            String errorMsg;
            if (count > 1) {
                errorMsg = "This image has " + count
                        + " missing values in its metadata: ";
            } else {
                errorMsg = "This image has a missing value in its metadata: ";
            }
            // Sorry for this quick "ArrayString to String" ;->
            errorMsg += errors.toString().replace("[", "").replace("]", "");
            throw new ClientException(errorMsg);
        }

        return inDoc;
    }
}
