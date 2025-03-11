package org.traccar.manhunt;

public class DeviceInfo {
    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private boolean isCaught;
    public boolean getIsCaught() { return isCaught; }
    public void setIsCaught(boolean isCaught) { this.isCaught = isCaught; }

}
