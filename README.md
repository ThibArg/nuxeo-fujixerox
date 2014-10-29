# nuxeo-fujixerox
===

Current version: 1.1.0 (2014-10-20)

This plug-in adds:

* **A listener ("About to create" and "before modification")** which checks the picture embedded in the nuxeo document has no resolution or no color space. If it is the case, the transaction is rolled back and an exception is raised, whose message explains the problem ("missing value: X-Resolution" for example)

* **A "Stored Picture Rendition" handler** which provide a pattern where the renditions for a document are pre-built and stored in the document itsef. This adds performance when a client 
needs to get a specific rendition: No need build it, it is already here. Technically speaking, we have:
  * An _event listener_:
    * Listens (asynchronously) to the "pictureViewsgenerationDone" event
    * Prebuilds _all_ the renditions contributed (via xml) for the `StoredPictureRenditionProvider` class (cd OSGI-INF/extensions/rendition-contrib.xml)
    * The renditions are stored in the `picture:views` schema
    * The name of the rendition is the key (title) of the view
  * A `RenditionProvider`, which:
    * Tells the `RenditionService` which renditions are available for current document
    * Just send the blob stored in the `picture:views` schema, given the name of the rendition
  * One can use this code as an example showing how to add new renditions and store them in the document. We currently handle only `Picture` documents (not `Video`, `Audio`, `File`, ...)
    * Three renditions are built (in version 1.1.0): `jpeg200x200`, `jpegWatermarked` and `imageToPDF`
    * The principle is the following. For each rendition:
      * An XML contribution is added to the `rendition-conrib.xml` file, declaring the `name` of the rendition (must be unique among the renditions), the `class` to use for the rendering (must be `org.nuxeo.fujixerox.storedpicturerendition.StoredPictureRenditionProvider`) and the `contentType` that will be output.
      * For this rendition, we also have a command-line contribution in `command-line-contrib.xml`. The main point is that the `name` of the command-line contribution must be exactly the same as the name of the contributed rendition. This is how the plugin generates the rendition (it loops thru each declared rendition and executes the command line of the same name)
    * The code contains comments explaining the behavior. Mainly,
      * To add a new rendition (or to change the way existing renditions are built):
        * Add the contributions to `rendition-conrib.xml` and `command-line-contrib.xml`
        * Handle the parameters for this contribution to `StoredPictureRenditionBuilder`
      * To add a notification once the renditions are built, or to change a flag in a schema, modify the code in `PictureViewsGenerationDoneListener` (there are comments telling you where to add your changes)
      * (no need to change `StoredPictureRenditionProvider`)
      

* **An operation, `ValidatePictureMetadata`** which check the Blob received in input, and applies the same validation. The operation accepts 2 parameters:
  * `varResult` (optional): The name of a Context variable that will be filled with the error message (not the full stack trace)
  * `throwException`: A boolean. If `true, an exception is raised in case of problem. Default value is `true`.
  * _NOTICE_: This operation is actullay not used in the `Nuxeotest` Studio project

Both classes call the `ValidatePictureMetadata` class which conteins the validation rules. it is this class that you should modify if you need to add rules for example.

As for other plug-ins, to use the operation in your Studio project, you need to add its JSON definition to Settings & Versioning > Registries > Automation Operations. You can add the following declaring:

```
 {
    "id" : "Blob.ValidatePictureMetadata",
    "label" : "Validate Picture Metadata",
    "category" : "Files",
    "description" : "This operation check if the input blob has <code>x/y resolution</code> and <code>colorspace</code> set. The (optionnal) <code>varResult</code> Context variable name will be filled with the string message (empty if no error). If the <code>throwException</code> box is checked, and exception is raised if the blob does not pass the validation (default is <code>true</code>.",
    "url" : "Blob.ValidatePictureMetadata",
    "requires" : null,
    "signature" : [ "blob", "blob", "bloblist", "bloblist" ],
    "params" : [ {
      "name" : "varResult",
      "type" : "string",
      "required" : false,
      "order" : 0,
      "widget" : null,
      "values" : [ ]
    }, {
      "name" : "throwException",
      "type" : "boolean",
      "required" : false,
      "order" : 0,
      "widget" : null,
      "values" : [ "true" ]
    } ]
  }
```


**Notice** The .zip of the marketplace package has been added to this repository. It is not 100% strict to put binaries outside the "releases" tab, but it is faster to get it. once we have a v1, we'll do egular releases


### IMPORTANT: DEPENDENCIES

Since we already have the code to read the metadata in the `nuxeo-imagemetadata-utils` plug-in, it was not duplicated here. This would add the hassle of handling the `im4java` library, handling duplicated code, etc. The problem is that, as of today (Oct. 2014), this `nuxeo-imagemetadata-utils` plug-in is not part of the nuxeo distribution, and so, it does not exist in the public maven repositories: You can't compile it with maven. In order to compile `nuxeo-fujixerox` and to use it in Eclipse you must have `nuxeo-imagemetadata-utils-plugin` maven artifact in _your_ local repository:

1. Download or fork `nuxeo-imagemetadata-utils` from [here](https://github.com/ThibArg/nuxeo-imagemetadata-utils).
2. Install it with maven (see the "Building the Plugin" part of its README file). This creates the artifcat in your local repository
  * You can now build `nuxeo-fujixerox`(see below)
3. Import its .jar in Eclipse

Once this step is done, you can build `nuxeo-fujixerox`:

```
cd /path/to/nuxeo-fujixerox
mvn clean install
```

**Using `nuxeo-fujixerox` in Eclipse**

Once it has been compiled with maven, you can generate the Eclipse project:

```
# Generate just for the plug-in itself. Useless to generate it for the MP or the root
cd /path/to/nuxeo-fujixerox-plugin
mvn eclipse:eclipse
```

Now, in eclipse, you can import the project.
