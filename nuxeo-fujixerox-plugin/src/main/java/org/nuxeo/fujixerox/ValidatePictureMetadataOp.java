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

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.RecoverableClientException;

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
 * Last, but still very important: The operation does not check the input blob
 * is an image, the caller is in charge of that.
 *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */
@Operation(id = ValidatePictureMetadataOp.ID, category = Constants.CAT_BLOB, label = "Validate Picture Metadata", description = "This operation check if the input blob has <code>x/y resolution</code> and <code>colorspace</code> set. The (optionnal) <code>varResult</code> Context variable name will be filled with the string message (empty if no error). If the <code>throwException</code> box is checked, and exception is raised if the blob does not pass the validation (default is <code>true</code>.")
public class ValidatePictureMetadataOp {

    public static final String ID = "Blob.ValidatePictureMetadata";

    // private static final Log log =
    // LogFactory.getLog(ValidatePictureMetadataOp.class);

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService automationService;

    @Param(name = "varResult", required = false)
    protected String varResult;

    @Param(name = "throwException", required = false, values = { "true" })
    protected boolean throwException = true;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws RecoverableClientException {

        String errorMsg = "";
        errorMsg = ValidatePictureMetadata.validate(inBlob);

        if (varResult != null && !varResult.isEmpty()) {
            ctx.put(varResult, errorMsg);
        }

        if (!errorMsg.isEmpty() && throwException) {
            throw new RecoverableClientException(errorMsg, errorMsg, null);
        }

        return inBlob;
    }
}
