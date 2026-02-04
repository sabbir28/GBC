package sabbir.apk.InterNet.Deta;

import java.time.LocalTime;

public class ScheduleItem {

    public String subject;
    public String instructor;
    public LocalTime start;
    public LocalTime end;
    public ClassState state;

    public boolean isCurrent(LocalTime now) {
        return !now.isBefore(start) && now.isBefore(end);
    }

    public boolean isPast(LocalTime now) {
        return now.isAfter(end);
    }
}
