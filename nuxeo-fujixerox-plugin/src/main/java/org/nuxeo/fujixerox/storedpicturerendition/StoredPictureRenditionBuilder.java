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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureViewImpl;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * This class builds one rendition for each XML rendition definition which
 * references {@link StoredPictureRenditionProvider} (see
 * OSGI-INF/extension/rendition-contrib.xml).
 * <p>
 * For each rendition, an entry is added to the <code>picture:views</code> field of the
 * document, the name of the rendition is the key to retrieve it. We are using
 * this field because nuxeo already has tools to make use of this field quite
 * easy ({@link MultiviewPicture}). So, this class calculates the rendition and
 * stores the result, while {@link StoredPictureRenditionProvider} just gets the
 * view based on the name.
 * <p>
 * <b>About names</b>: The main point here is that <b>the name of the rendition
 * must strictly matches the name of the command-line contribution</b>. So for
 * example, we have the "jpeg200x200" rendition, and the "jpeg200x200"
 * command-line. See rendition-contrib.xml and command-line-contrib.xml.
 * <p>
 * <p>
 * <b>IMPORTANT</b>: Because <code>picture:views</code> is used by the core platform to store
 * pre-calculated views of the image, be careful on the names of your
 * renditions: <b>You must not use the same names as the one use by the
 * platform</b>. See the <code>DefaultPictureAdapter</code> class in nuxeo source/doc and its
 * <code>computeDefaultPictureTemplates()</code> method. The reserved named as
 * of today (Oct. 2014, but very unlikely to change) are:
 * <p>
 * <ul>
 * <li>"Medium"</li>
 * <li>"Original"</li>
 * <li>"Small"</li>
 * <li>"Thumbnail"</li>
 * <li>"OriginalJpeg"</li>
 * </ul>
 * <p>
 *
 * @since 5.9.5
 */
public class StoredPictureRenditionBuilder {

    static private Log log = LogFactory.getLog(StoredPictureRenditionBuilder.class);

    static public String TEMP_FILE_PREFIX = "RendHdler-";

    protected DateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

    // We cache the availability here
    static protected HashMap<String, Boolean> availableCommandLines = new HashMap<String, Boolean>();

    protected DocumentModel doc;

    public StoredPictureRenditionBuilder(DocumentModel inDoc) {

        doc = inDoc;
    }

    protected boolean isCommanLineAvailable(String inName) {

        Boolean isAvailable = availableCommandLines.get(inName);

        if (isAvailable == null) {
            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);

            CommandAvailability ca = cles.getCommandAvailability(inName);

            isAvailable = ca.isAvailable();
            availableCommandLines.put(inName, isAvailable);

        }

        return isAvailable.booleanValue();
    }

    /*
     * Utility getting the filePath of the blob if possible, to avoid creating a
     * temp. document in all cases. In our context, we should be pretty
     * confident we have a StorageBlob, but let's be prepared for wider context.
     */
    protected String getFilePath(Blob inBlob) throws IOException {
        // We try to directly get the full path of the binary, if possible
        String filePath = "";

        try {
            File f = BlobHelper.getFileFromBlob(inBlob);
            filePath = f.getAbsolutePath();
        } catch (Exception e) {
            filePath = "";
        }

        if (filePath.isEmpty()) {
            File tempFile = File.createTempFile(TEMP_FILE_PREFIX, "");
            inBlob.transferTo(tempFile);
            filePath = tempFile.getAbsolutePath();
            tempFile.deleteOnExit();
            Framework.trackFile(tempFile, this);
        }

        return filePath;
    }

    public void buildAvailableRenditions() throws CommandNotAvailable,
            IOException, CommandException {

        // Build renditions declared by our PictureRenditionProvider (see
        // rendition-contrib.xml)
        // IMPORTANT: We must set the targetFilePath with a correct extension,
        // so ImageMagick can convert to jpeg, pdf, ...
        RenditionService renditionService = Framework.getService(RenditionService.class);
        List<RenditionDefinition> defs = renditionService.getDeclaredRenditionDefinitionsForProviderType(StoredPictureRenditionProvider.class.getSimpleName());
        for (RenditionDefinition oneDef : defs) {
            // IMPORTANT TO REMEMBER: The name of the rendition is the same as
            // the name of the command line contribution
            String renditionName = oneDef.getName();

            if (isCommanLineAvailable(renditionName)) {

                // We need to set the parameters as expected by the command line
                // In all cases, we need to setup the sourceFilePath and the
                // targetFilePathNoExtension parameters
                Blob mainBlob = doc.getAdapter(BlobHolder.class).getBlob();
                String sourceFilePath = getFilePath(mainBlob);

                CmdParameters params = new CmdParameters();
                params.addNamedParameter("sourceFilePath", sourceFilePath);

                // Now some more specific parameters
                File tempDestFile;
                String destFileExtension;
                switch (renditionName) {

                case "jpeg200x200":
                    destFileExtension = ".jpeg";
                    break;

                case "jpegWatermarked":
                    destFileExtension = ".jpeg";
                    // This is an example. The values could have been hard-coded
                    // in the command line. The command line definition is:
                    // "#{sourceFilePath}" -gravity SouthWest -fill #{textColor}
                    // -stroke #{strokeColor} -strokewidth #{strokeWidth}
                    // -pointsize #{textSize} -annotate
                    // #{textRotation}x#{textRotation}+#{xOffset}+#{yOffset}
                    // #{textValue} "#{targetFilePath}.jpeg"
                    params.addNamedParameter("gravity", "SouthWest");
                    params.addNamedParameter("textColor", "red");
                    params.addNamedParameter("strokeColor", "black");
                    params.addNamedParameter("strokeWidth", "1");
                    params.addNamedParameter("textSize", "24");
                    params.addNamedParameter("textRotation", "0");
                    params.addNamedParameter("xOffset", "0");
                    params.addNamedParameter("yOffset", "0");
                    Calendar creationDate = (Calendar) doc.getPropertyValue("dc:created");
                    params.addNamedParameter(
                            "textValue",
                            "Created "
                                    + new SimpleDateFormat("yyyy-MM-dd").format(creationDate.getTime()));
                    break;

                case "imageAsPDF":
                    destFileExtension = ".pdf";
                    break;

                // . . . . . . . . . . . . . .
                // . . . other renditions . . .
                // . . . . . . . . . . . . . .

                default:
                    throw new ClientException("Rendition <" + renditionName
                            + "> not handled in the code.");
                }

                tempDestFile = File.createTempFile(TEMP_FILE_PREFIX,
                        destFileExtension);
                tempDestFile.deleteOnExit();
                Framework.trackFile(tempDestFile, this);
                params.addNamedParameter("targetFilePath",
                        tempDestFile.getAbsolutePath());

                // Run
                CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
                ExecResult result = cles.execCommand(renditionName, params);

                // Give up the whole loop in case of problem? => Business rule
                // to be adapted
                if (result.getError() != null) {
                    throw new ClientException("Failed to execute the command <"
                            + renditionName + ">", result.getError());
                }

                if (!result.isSuccessful()) {
                    throw new ClientException("Failed to execute the command <"
                            + renditionName + ">. Final command [ "
                            + result.getCommandLine()
                            + " ] returned with error "
                            + result.getReturnCode());
                }

                // Setup the blob before adding it to the views. It needs a
                // correct mime-type and a correct file name. the filename
                // should be the original filename, not
                // the name from tempDestFile
                FileBlob resultBlob = new FileBlob(tempDestFile);
                resultBlob.setMimeType(oneDef.getContentType());
                resultBlob.setFilename(mainBlob.getFilename()
                        + destFileExtension);

                addBlobToViews(resultBlob, renditionName);

                // ***Do not cleanup*** tempDestFile. The FileBlob still
                // references this File and is requested by the caller, when it
                // save() the document. Do not:
                // tempDestFile.delete();

            } else {
                // Lets report the problem, and do not throwing an error? +> to
                // be adapt with business rules.
                log.warn("Cannot create the <"
                        + renditionName
                        + " rendition because the corresponding command line is not available.");
            }
        }

/**********************************************************************
        This approach just can't work. In the best case it will create a
        useless load in the database, but mainly it makes it required to
        filter the generated renditions, which are kind of related document
        acting as a version, but not really a version, but etc. etc. Not
        recommended for a pre-built, pre-stored renditions approach.
        <code>
        RenditionService rs = Framework.getService(RenditionService.class);
        List<RenditionDefinition> allDefs = rs.getAvailableRenditionDefinitions(doc);
        for (RenditionDefinition oneDef : allDefs) {
            // Waiting for the "pdf" rendition to work also on pictures,
            // let's ignore it (it will fail in all cases)
            if (!oneDef.getName().equals("pdf")) {
                try {
                    rs.storeRendition(doc, oneDef.getName());
                } catch (Exception e) {
                    log.warn("Can't generate rendition <" + oneDef.getName()
                            + "> for document id " + doc.getId(), e);
                }
            }
        }
        </code>
         **********************************************************************/

    }

    /*
     * Save the blob generated by the command line as a new entry in the
     * picture:views field. Thankfully, nuxeo already provides utility classes
     * for this purpose (PictureView and MultiviewPicture)
     */
    protected void addBlobToViews(Blob inBlob, String inRenditionName) {

        PictureViewImpl view = new PictureViewImpl();
        // Name is the name of the rendition
        // This is a very important field: The PictureRenditionProvider
        // class uses this name/title to return the pre-calculated
        // rendition
        view.setTitle(inRenditionName);
        // Add the blob
        view.setContent(inBlob);
        // Quicker access to the file name (no need to get to the blob
        // definition)
        view.setFilename(inBlob.getFilename());

        // Optional, but could be used for reporting, query, ...
        // For example, let's use the rendition name
        view.setDescription("Pre-built Rendition for " + inRenditionName);
        view.setTag(inRenditionName);

        // Last info. This requires extra work from ImageMagick and is
        // optional
        ImagingService imagingService = Framework.getService(ImagingService.class);
        try {
            ImageInfo info = imagingService.getImageInfo(inBlob);
            view.setWidth(info.getWidth());
            view.setHeight(info.getHeight());
        } catch(Exception e) {
            // We just ignore the error
        }

        // We are all set, let's save the rendition
        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
        mvp.addView(view);
    }
}
