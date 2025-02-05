#### TASK 002: Add "open file location" in the right click menu of files in the project panel.  
>  Also remove "Use as workspace" if it's not a directory.
- Self explanatory. This is all within `ProjectPanel.java`, it'd be nice to open the system file explorer (xdg-open in linux, explorer on Windows?)

On a similar note, we should add `Right Click > New Folder (or File) in the project panel.` So under folder right-click context menu, we'll have `New Folder`, `New File`, `Use as workspace`, `Open File Location. Open file location should have a split beforehand. For File right-click ctx menu, we'll just have New Folder, new File, Open File Location.`

----

#### TASK 003: The file viewer should limit the filesize to a certain number of characters, or maybe check if the formatter is the performance bottleneck.
 - Self explanatory. Check the size of the file (if not an image) in FileViewerPanel before opening. Maybe a third panel that says "File is too large." if its over like ~100kb? Idk what the largest text file realistically will be opened. Maybe we prompt a dialog (Allow large file).

---

#### TASK 004: The New Project system is a bit clunky, maybe we can make it more intuitive.  

The `New Project` option should replace the Temp Project option, except it just creates a new directory within a `VaiProjects` folder somewhere on the PC, probably user home. This should be configurable, of course, but for now let's just have that path in `Constants.java`. 

So the user clicks **New Project...**, they're prompted for a project name (with a grayed out default already written in the text field, which clears when clicked), and it creates a new directory within the VaiProjects folder and opens it as a project. We'll remove the Temp Project option in favor of this. 

The randomly generated name should be a random noun, adjective, etc styled name generator.

----

#### TASK 005: The OpenFileDialog in Java is not ideal.
> If possible, we should replace the file dialog with something better than the Java one, since it has no bookmarked directories (Documents, Home, etc). This is only used for Open Project...

---

#### TASK 006: Prompt text area should be resizable
> At the moment it's a fixed size @ the bottom of the window.

- Self explanatory, ClientFrame, we need a split thing between the text area and the FileViewerPanel, allowing users to resize.


---

#### TASK 007: Save the last model used.
> The last model that was selected previously should be the default (instead of `o3-mini`) when initializing the UI.

---

#### TASK 008: Include the current open file in file viewer in the title.
> Instead of the project path, place the open file name there. If no file is open, just have the workspace path.

---



