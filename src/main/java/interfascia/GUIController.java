// Interfascia ALPHA 004 -- http://interfascia.plusminusfive.com/
// GUI Library for Processing -- http://www.processing.org/
//
// Copyright (C) 2006-2016 Brendan Berg
// interfascia (at) plusminusfive (dot) com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA
// --------------------------------------------------------------------
//
// Updated for Processing 3 by Anna Terzaroli 2015
// anna.giw (at) libero (dot) it
//

package interfascia;

import processing.core.PApplet;
import processing.event.KeyEvent;

import java.awt.*;
import java.awt.datatransfer.*;

public class GUIController extends GUIComponent implements ClipboardOwner {
    private GUIComponent[] contents;
    private int numItems = 0;
    private int focusIndex = -1;
    private boolean visible;
    private IFLookAndFeel lookAndFeel;
    public IFPGraphicsState userState;
    private Clipboard clipboard;

    public PApplet parent;

    public boolean showBounds = false;

    public GUIController(final PApplet newParent) {
        this(newParent, true);
    }

    public GUIController(final PApplet newParent, final int x, final int y, final int width, final int height) {
        this(newParent, true);
        setPosition(x, y);
        setSize(width, height);
    }

    public GUIController(final PApplet newParent, final boolean newVisible) {
        setParent(newParent);
        setVisible(newVisible);
        contents = new GUIComponent[5];

        lookAndFeel = new IFLookAndFeel(parent, IFLookAndFeel.DEFAULT);
        userState = new IFPGraphicsState();

        final SecurityManager security = System.getSecurityManager();
        if (security != null) {
            try {
                security.checkSystemClipboardAccess();
                clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (final SecurityException e) {
                clipboard = new Clipboard("Interfascia Clipboard");
            }
        } else {
            try {
                clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (final Exception e) {
                // THIS IS DUMB
            }
        }

        newParent.registerMethod("keyEvent", this);
        newParent.registerMethod("draw", this);
    }

    public void setLookAndFeel(final IFLookAndFeel lf) {
        lookAndFeel = lf;
    }

    public IFLookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    public void add(final GUIComponent component) {
        if (numItems == contents.length) {
            final GUIComponent[] temp = contents;
            contents = new GUIComponent[contents.length * 2];
            System.arraycopy(temp, 0, contents, 0, numItems);
        }
        component.setController(this);
        component.setLookAndFeel(lookAndFeel);
        //component.setIndex(numItems);
        contents[numItems++] = component;
        component.initWithParent();
    }

    public void remove(final GUIComponent component) {
        int componentIndex = -1;

        for (int i = 0; i < numItems; i++) {
            if (component == contents[i]) {
                componentIndex = i;
                break;
            }
        }

        if (componentIndex != -1) {
            contents[componentIndex] = null;
            if (componentIndex < numItems - 1) {
                System.arraycopy(contents, componentIndex + 1, contents, componentIndex, numItems - (componentIndex + 1));
            }
            numItems--;
        }
    }

    public void setParent(final PApplet argParent) {
        parent = argParent;
    }

    public PApplet getParent() {
        return parent;
    }

    public void setVisible(final boolean newVisible) {
        visible = newVisible;
    }

    public boolean getVisible() {
        return visible;
    }

    public void requestFocus(final GUIComponent c) {
        for (int i = 0; i < numItems; i++) {
            if (c == contents[i])
                focusIndex = i;
        }
    }

    // ****** LOOK AT THIS, I DON'T THINK IT'S RIGHT ******
    public void yieldFocus(final GUIComponent c) {
        if (focusIndex > -1 && focusIndex < numItems && contents[focusIndex] == c) {
            focusIndex = -1;
        }
    }

    public GUIComponent getComponentWithFocus() {
        return contents[focusIndex];
    }

    public boolean getFocusStatusForComponent(final GUIComponent c) {
        if (focusIndex >= 0 && focusIndex < numItems)
            return c == contents[focusIndex];
        else
            return false;
    }

    public void lostOwnership(final Clipboard parClipboard, final Transferable parTransferable) {
        // System.out.println ("Lost ownership");
    }

    public void copy(final String v) {
        final StringSelection fieldContent = new StringSelection(v);
        clipboard.setContents(fieldContent, this);
    }

    public String paste() {
        final Transferable clipboardContent = clipboard.getContents(this);

        if ((clipboardContent != null) &&
            (clipboardContent.isDataFlavorSupported(DataFlavor.stringFlavor))) {
            try {
                final String tempString;
                tempString = (String) clipboardContent.getTransferData(DataFlavor.stringFlavor);
                return tempString;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public void keyEvent(final KeyEvent e) {
        if (visible) {
            if (e.getAction() == KeyEvent.PRESS && e.getKeyCode() == java.awt.event.KeyEvent.VK_TAB) {
                if (focusIndex != -1 && contents[focusIndex] != null) {
                    contents[focusIndex].actionPerformed(
                        new GUIEvent(contents[focusIndex], "Lost Focus")
                    );
                }

                if (e.isShiftDown())
                    giveFocusToPreviousComponent();
                else
                    giveFocusToNextComponent();

                if (focusIndex != -1 && contents[focusIndex] != null) {
                    contents[focusIndex].actionPerformed(
                        new GUIEvent(contents[focusIndex], "Received Focus")
                    );
                }

            } else if (e.getKeyCode() != java.awt.event.KeyEvent.VK_TAB) {
                if (focusIndex >= 0 && focusIndex < contents.length)
                    contents[focusIndex].keyEvent(e);
            }
        }
    }

    private void giveFocusToPreviousComponent() {
        final int oldFocus = focusIndex;
        focusIndex = (focusIndex - 1) % numItems;
        while (!contents[focusIndex].canReceiveFocus() && focusIndex != oldFocus) {
            focusIndex = (focusIndex - 1) % numItems;
        }
    }

    private void giveFocusToNextComponent() {
        final int oldFocus = focusIndex;
        focusIndex = (focusIndex + 1) % numItems;
        while (!contents[focusIndex].canReceiveFocus() && focusIndex != oldFocus) {
            focusIndex = (focusIndex + 1) % numItems;
        }
    }

    public void draw() {
        if (visible) {
            userState.saveSettingsForApplet(parent);
            lookAndFeel.defaultGraphicsState.restoreSettingsToApplet(parent);
            //parent.background(parent.g.backgroundColor);
            parent.fill(parent.color(0));
            parent.rect(getX(), getY(), getWidth(), getHeight());
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    //parent.smooth();
                    contents[i].draw();
                }
            }
            userState.restoreSettingsToApplet(parent);
        }
    }
}
