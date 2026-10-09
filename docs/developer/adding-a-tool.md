# Adding a Toolbox tool

A Toolbox tool is a Swing panel that receives the images selected in the file
table (or a pixel cutout from ds9) and shows a result. The Toolbox
(`ImageToolBoxPanel`) has a column of buttons on the left and a stack of tool
panels on the right (`MultiFlickPanel`), of which one is visible at a time.

## 1. Write the tool

Subclass
[`ImageEvaluator`](https://github.com/drhaz/VisualFitsBrowser/blob/master/src/main/java/org/cowjumping/VisualFitsBrowser/ImageActions/ImageEvaluator.java)
in the package `org.cowjumping.VisualFitsBrowser.ImageActions`, and override one or
both of its input methods:

- `setImageList(Vector<FitsFileEntry>)` — called with the files selected in the
  table. `FitsFileEntry` gives you the path (`getAbsolutePath()`) and the parsed
  header values (`ObjName`, `ExpTime`, `Filter`, `DateObs`, ...).
- `setImageContainer(Vector<ImageContainer>)` — called with pixel data, for
  example a cutout fetched from ds9.

Long calculations must not block the Swing event thread. Submit them to the
shared `myThreadPool` and update the GUI with `SwingUtilities.invokeLater`.

```java
package org.cowjumping.VisualFitsBrowser.ImageActions;

import java.awt.BorderLayout;
import java.util.Vector;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.cowjumping.VisualFitsBrowser.util.FitsFileEntry;

/** Shows the total exposure time of the selected images. */
public class ExposureSummary extends ImageEvaluator {

    private final JTextArea output = new JTextArea();

    public ExposureSummary() {
        super();
        setLayout(new BorderLayout());
        add(output, BorderLayout.CENTER);
    }

    @Override
    public int setImageList(Vector<FitsFileEntry> images) {
        if (images == null)
            return -1;
        myThreadPool.submit(() -> {
            double total = 0;
            for (FitsFileEntry e : images)
                if (e.ExpTime != null && !e.ExpTime.isNaN())
                    total += e.ExpTime;
            final String text = String.format("%d images, %.1f s total", images.size(), total);
            SwingUtilities.invokeLater(() -> output.setText(text));
        });
        return images.size();
    }
}
```

## 2. Register it in the Toolbox

All changes are in
[`ImageToolBoxPanel`](https://github.com/drhaz/VisualFitsBrowser/blob/master/src/main/java/org/cowjumping/VisualFitsBrowser/ImageActions/ImageToolBoxPanel.java):

1. Add a field and a panel name:

    ```java
    ExposureSummary myExposureSummary = null;
    private static final String EXPOSUREPANEL = "EXPOSUREVIEW";
    ```

2. Create it and add it to the panel stack in `fillMultiPanelView()`:

    ```java
    myExposureSummary = new ExposureSummary();
    myExposureSummary.setName(EXPOSUREPANEL);
    myMultiPanel.add(myExposureSummary);
    ```

3. Add a button in `fillButtonPanel()` that brings the panel to the front and
   passes it the current selection:

    ```java
    JButton exposureButton = new JButton("Exposure sum");
    exposureButton.addActionListener(e -> {
        myMultiPanel.setTopComponent(EXPOSUREPANEL);
        myExposureSummary.setImageList(getFBPSelected());
    });
    ButtonPanel.add(exposureButton);   // before the vertical glue
    ```

4. *(Optional)* To update the tool whenever the table selection changes, add a
   branch to `pushFileSelection()`. Note that the file table currently calls it
   only when exactly one row is selected.

## 3. Document it

Add a section to [`docs/user-guide/toolbox.md`](../user-guide/toolbox.md) in the
same pull request.
