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

package org.nuxeo.fujixerox.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.fujixerox.ValidatePictureMetadataOp;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.commandline.executor",
        "nuxeo-imagemetadata-utils", "nuxeo-fujixerox-plugin" })
public class NuxeoFujiXeroxTest {

    private static final String IMAGE_OK = "images/image-ok.jpg";

    private static final String IMAGE_NOT_OK = "images/image-not-ok.jpg";

    protected DocumentModel parentOfTestDocs;

    protected DocumentModel docOK;

    protected DocumentModel docNotOK;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService service;

    @Before
    public void setUp() {

        parentOfTestDocs = coreSession.createDocumentModel("/",
                "test-pictures", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-pictures");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);
        parentOfTestDocs = coreSession.saveDocument(parentOfTestDocs);
        assertNotNull(parentOfTestDocs);

        File aFile = FileUtils.getResourceFileFromContext(IMAGE_OK);
        docOK = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), aFile.getName(), "Picture");
        docOK.setPropertyValue("dc:title", aFile.getName());
        docOK.setPropertyValue("file:content", new FileBlob(aFile));
        docOK = coreSession.createDocument(docOK);
        docOK = coreSession.saveDocument(docOK);
        assertNotNull(docOK);

        aFile = FileUtils.getResourceFileFromContext(IMAGE_NOT_OK);
        docNotOK = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), aFile.getName(), "Picture");
        docNotOK.setPropertyValue("dc:title", aFile.getName());
        docNotOK.setPropertyValue("file:content", new FileBlob(aFile));
        docNotOK = coreSession.createDocument(docNotOK);
        docNotOK = coreSession.saveDocument(docNotOK);
        assertNotNull(docNotOK);
    }

    @After
    public void cleanup() {
        coreSession.removeDocument(parentOfTestDocs.getRef());
        coreSession.save();
    }

    @Test
    public void testValidatePictureMetadataOperation() throws Exception {

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        // ========================================
        // TEST DOC OK => NO Exception EXPECTED
        // ========================================
        System.out.println("Testing on a doc with valid metadata...");
        ctx.setInput(docOK);
        chain = new OperationChain("testChain");
        chain.add(ValidatePictureMetadataOp.ID);
        try {
            service.run(ctx, chain);
        } catch (Exception e) {
            assertTrue(
                    "This document is ok, should not have raised an exception",
                    false);
        }



        // ========================================
        // TEST DOC *NOT* OK => Exception EXPECTED
        // ========================================
        System.out.println("Testing on a doc with invalid metadata...");
        ctx.setInput(docNotOK);
        chain = new OperationChain("testChain2");
        chain.add(ValidatePictureMetadataOp.ID);
        try {
            service.run(ctx, chain);
            assertTrue(
                    "This document is *not* ok => should have raised an exception",
                    false);
        } catch (Exception e) {
            // Check we have our error
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            assertTrue(exceptionAsString.indexOf("This image has 2 missing values in its metadata: X-Resolution, Y-Resolution") > -1);
        }

    }
}