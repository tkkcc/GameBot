package android.os;

import java.io.FileDescriptor;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(MemoryFile.class)
public class MemoryFileHidden {

    public FileDescriptor getFileDescriptor() {
        return null;
    }
}
