# nuxeo-fujixerox
===


This plug-in adds an operation, `ValidatePictureMetadata` which throws an explicit error if the picture embedded in the nuxeo document has no resolution or no color space.

As for other plug-ins, to use the operation in your Studio project, you need to add its JSON definition to Settings & Versioning > Registries > Automation Operations. You can add the following declaring:

```
{
  "id" : "ValidatePictureMetadataOp",
  "label" : "Validate Picture Metadata",
  "category" : "Document",
  "description" : "This operation calls the <code>ExtractMetadataInDocument</code> operation from <code>nuxeo-imagemetadata-utils</code>, and thorw an exsception if some metadata is missing",
  "url" : "ValidatePictureMetadataOp",
  "requires" : null,
  "signature" : [ "document", "document", "documents", "documents" ],
  "params" : [ ]
}
```

A typical usage would be in the "About to Create" event, when a document has the "picture" facet. In the chain bound to this event, just drag-drop this operaiton (Document > Validate Picture Metadata): When creating a new document, if the embedded image does not have the required metadata, the exception will be triggerer, and the document will not be created (rollback of the transaction)

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
