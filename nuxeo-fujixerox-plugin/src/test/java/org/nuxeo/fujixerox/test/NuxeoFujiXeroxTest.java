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
import java.util.ArrayList;
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
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.fujixerox.ValidatePictureMetadata;
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
        "org.nuxeo.ecm.platform.rendition.core",
        "nuxeo-imagemetadata-utils", "nuxeo-fujixerox-plugin" })
public class NuxeoFujiXeroxTest {

    private static final String IMAGE_OK = "images/image-ok.jpg";

    private static final String IMAGE_NOT_OK = "images/image-not-ok.jpg";

    private static final String IMAGE_FOR_RENDITION = "images/for-test-rendition.jpg";

    // Initialized in setup()
    private static ArrayList<String> RENDITION_NAMES = new ArrayList<String>();

    protected DocumentModel parentOfTestDocs;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Inject
    EventService eventService;

    @Inject
    RenditionService renditionService;

    @Before
    public void setUp() {

        RENDITION_NAMES.add("jpeg200x200");
        RENDITION_NAMES.add("jpegWatermarked");
        RENDITION_NAMES.add("imageAsPDF");

        parentOfTestDocs = coreSession.createDocumentModel("/",
                "test-pictures", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-pictures");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);
        parentOfTestDocs = coreSession.saveDocument(parentOfTestDocs);
        assertNotNull(parentOfTestDocs);
    }

    @After
    public void cleanup() {
        coreSession.removeDocument(parentOfTestDocs.getRef());
        coreSession.save();
    }

    protected DocumentModel createPictureDocumentModel(String inImageResource) {

        DocumentModel aDoc;

        File aFile = FileUtils.getResourceFileFromContext(inImageResource);
        aDoc = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), aFile.getName(), "Picture");
        aDoc.setPropertyValue("dc:title", aFile.getName());
        aDoc.setPropertyValue("file:content", new FileBlob(aFile));

        return aDoc;
    }

    /*
    @Test
    public void testStoredPictureRendition() throws Exception {

        System.out.println("Check testStoredPictureRendition...");
        DocumentModel doc = createPictureDocumentModel(IMAGE_FOR_RENDITION);

        doc = coreSession.createDocument(doc);
        doc = coreSession.saveDocument(doc);

        eventService.waitForAsyncCompletion();

        // Check the picture:views have been created
        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
        for(String aName : RENDITION_NAMES) {
            PictureView v = mvp.getView(aName);
            assertNotNull(aName + " should be in the picture:views field", v);
            assertNotNull("Blob for " + aName + " should not be null", v.getBlob());
        }


        // Now, check the renditions are available
        List<Rendition> renditions = renditionService.getAvailableRenditions(doc);
        // If you add renditions, think about modifying RENDITION_NAMES in setup()
        assertEquals(RENDITION_NAMES.size(), renditions.size());
        for(String aName : RENDITION_NAMES) {
            boolean found = false;
            for(Rendition r : renditions) {
                if(r.getName().equals(aName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("The rendition " + aName + " was not found", found);
        }

        // Now, check we have valid blob
        for(Rendition r : renditions) {
            assertNotNull("Blob for rendition " + r.getName() + " should not be null", r.getBlob());
        }
    }
    */

    @Test
    public void testValidatePictureMetadata() throws Exception {
        // This is the test of the core validation class, which is called by the others
        File aFile;
        FileBlob fb;
        String errorMsg = "";

        // ========================================
        // TEST DOC OK => NO Exception EXPECTED
        // ========================================
        System.out.println("Check ValidatePictureMetadata on a picture with valid metadata...");
        aFile = FileUtils.getResourceFileFromContext(IMAGE_OK);
        fb = new FileBlob(aFile);
        errorMsg = ValidatePictureMetadata.validate(fb);
        assertTrue(errorMsg.isEmpty());


        // ========================================
        // TEST DOC *NOT* OK => Exception EXPECTED
        // ========================================
        System.out.println("Check ValidatePictureMetadata on a picture with missing metadata...");
        errorMsg = "";
        aFile = FileUtils.getResourceFileFromContext(IMAGE_NOT_OK);
        fb = new FileBlob(aFile);
        errorMsg = ValidatePictureMetadata.validate(fb);
        // This IMAGE_NOT_OK image has colorspace but no resolution
        // Just checking labels, because we may change the format/sentence
        assertTrue(errorMsg.indexOf("X-Resolution") > -1);
        assertTrue(errorMsg.indexOf("Y-Resolution") > -1);

    }

    @Test
    public void testValidatePictureMetadataListener() throws Exception {

        DocumentModel docOK, docNotOK;

        // ========================================
        // TEST DOC OK => NO Exception EXPECTED
        // ========================================
        System.out.println("Check ValidatePictureMetadataListener on a picture with valid metadata...");
        docOK = createPictureDocumentModel(IMAGE_OK);
        try {
            docOK = coreSession.createDocument(docOK);
            docOK = coreSession.saveDocument(docOK);
        } catch (Exception e) {
            assertTrue(
                    "This document is ok, should not have raised an exception",
                    false);
        }

        // ========================================
        // TEST DOC *NOT* OK => Exception EXPECTED
        // ========================================
        System.out.println("Check ValidatePictureMetadataListener on a picture with valid missing metadata...");
        docNotOK = createPictureDocumentModel(IMAGE_NOT_OK);
        try {
            docNotOK = coreSession.createDocument(docNotOK);
            docNotOK = coreSession.saveDocument(docNotOK);
            assertTrue(
                    "This document is *not* ok => should have raise an exception",
                    false);
        } catch(Exception e) {
         // Check we have our error
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            // This IMAGE_NOT_OK image has colorspace but no resolution
            // Just checking labels, because we may change the format/sentence
            assertTrue(exceptionAsString.indexOf("X-Resolution") > -1);
            assertTrue(exceptionAsString.indexOf("Y-Resolution") > -1);
        }

    }

    @Test
    public void testValidatePictureMetadataOperation() throws Exception {

        File aFile;
        FileBlob fb;

        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        // ========================================
        // TEST DOC OK => NO Exception EXPECTED
        // ========================================
        System.out.println("Check " + ValidatePictureMetadataOp.ID + " operation on a picture with valid metadata...");
        aFile = FileUtils.getResourceFileFromContext(IMAGE_OK);
        fb = new FileBlob(aFile);
        ctx.setInput(fb);
        chain = new OperationChain("testChain");
        // We get the error (expecting "" in this test) in the context
        ctx.put("resultError", "");
        // We let the default "throwException" value (set to true)
        chain.add(ValidatePictureMetadataOp.ID).set("varResult", "resultError");
        try {
            automationService.run(ctx, chain);
            String resultStr = (String) ctx.get("resultError");
            assertEquals("", resultStr);
        } catch (Exception e) {
            assertTrue(
                    "This document is ok, should not have raised an exception",
                    false);
        }



        // ========================================
        // TEST DOC *NOT* OK => Exception EXPECTED
        // ========================================
        System.out.println("Check " + ValidatePictureMetadataOp.ID + " operation on a picture with missing metadata...");
        aFile = FileUtils.getResourceFileFromContext(IMAGE_NOT_OK);
        fb = new FileBlob(aFile);
        ctx.setInput(fb);
        ctx.put("resultError", "");
        chain = new OperationChain("testChain2");
        // We let the default "throwException" value (set to true)
        chain.add(ValidatePictureMetadataOp.ID).set("varResult", "resultError");;
        try {
            automationService.run(ctx, chain);
            assertTrue(
                    "This document is *not* ok => should have raised an exception",
                    false);
        } catch (Exception e) {
            String resultStr = (String) ctx.get("resultError");
            assertTrue(resultStr.indexOf("X-Resolution") > -1);
            assertTrue(resultStr.indexOf("Y-Resolution") > -1);

            // Check we have our error
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            assertTrue(exceptionAsString.indexOf("X-Resolution") > -1);
            assertTrue(exceptionAsString.indexOf("Y-Resolution") > -1);
        }

    }
}