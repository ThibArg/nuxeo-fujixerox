package org.nuxeo.fujixerox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.im4java.core.InfoException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.imagemetadata.ImageMetadataReader;
import org.nuxeo.imagemetadata.XYResolutionDPI;
import org.nuxeo.imagemetadata.ImageMetadataConstants.KEYS;

public class ValidatePictureMetadata {

    /*
     * This is where you can add your business rules for validation. here, we
     * just check resolution and colorspace.
     */
    public static String validate(Blob inBlob) {

        String errorMsg = "";

        try {
            // Read the Metadata
            ImageMetadataReader imdr;
            imdr = new ImageMetadataReader(inBlob);
            HashMap<String, String> result = null;
            // ==================================================
            // . . .Here, you could adapt and add more business rules about
            // metadata
            // validation. . .
            // ==================================================
            String[] keysStr = { KEYS.COLORSPACE, KEYS.RESOLUTION, KEYS.UNITS };
            result = imdr.getMetadata(keysStr);
            // Resolution needs extra work
            XYResolutionDPI dpi = new XYResolutionDPI(
                    result.get(KEYS.RESOLUTION), result.get(KEYS.UNITS));

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
            // ==================================================
            // . . . add your other rules here . . .
            // ==================================================

            int count = errors.size();
            if (count > 0) {
                if (count > 1) {
                    errorMsg = "This image has " + count
                            + " missing values in its metadata: ";
                } else {
                    errorMsg = "This image has a missing value in its metadata: ";
                }
                // Sorry for this quick "ArrayString to String" ;->
                errorMsg += errors.toString().replace("[", "").replace("]", "");
            }

        } catch (IOException | InfoException e) {
            errorMsg = e.getMessage();
        }

        return errorMsg;
    }
}
