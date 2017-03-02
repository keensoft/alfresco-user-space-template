
Alfresco User Space Template
============================

Adds the ability to create a default set of folders to an Alfresco user home by leveraging Space Templates.

After installing this AMP in your Alfresco WAR, you can create a folder under Data Dictionary for user home templates. Then, anything you put in that folder will be copied into the user home folder for new users.

If you add rules to your Template, those rules will be also copied to the new user home folder when the user is created.

**License**
The plugin is licensed under the [LGPL v3.0](http://www.gnu.org/licenses/lgpl-3.0.html). 

**State**
Current addon release is 1.0.0

**Compatibility**
The current version has been developed using Alfresco 5.0 and Alfresco SDK 2.1.1, although it runs also in Alfresco 5.1 and Alfresco 5.2

***No original Alfresco resources have been overwritten***


Downloading the ready-to-deploy-plugin
--------------------------------------
The binary distribution is made of one amp file to be deployed in Alfresco as a repo module:

* [repo AMP](https://github.com/keensoft/user-space-template-repo/releases/download/1.0.0/user-space-template-repo.amp)

You can install them by using standard [Alfresco deployment tools](http://docs.alfresco.com/community/tasks/dev-extensions-tutorials-simple-module-install-amp.html) in `alfresco.war`


Building the artifacts
----------------------
If you are new to Alfresco and the Alfresco Maven SDK, you should start by reading [Jeff Potts' tutorial on the subject](http://ecmarchitect.com/alfresco-developer-series-tutorials/maven-sdk/tutorial/tutorial.html).

You can build the artifacts from source code using maven
```$ mvn clean package```


Configuration
-------------

After installation and before starting Alfresco, following property should be included in **alfresco-global.properties**

```
// Folder to be used as template for user home folder creation
userhome.template.path=/app:company_home/app:dictionary/cm:User_x0020_home_x0020_template
```


Contributors
------------

This addon was developed during [BeeCon 2017 warm up session](http://www.keensoft.es/1-marzo-beecon-2017-warm-up-session-at-etopia/) at *Etopia Center for Art and Technology*

* Angel Borroy
* Guillermo Castillo
* José Antonio Matute
* Daniel E. Fernández
* Víctor Martínez
* José Lisbona
* Sergio Escobedo