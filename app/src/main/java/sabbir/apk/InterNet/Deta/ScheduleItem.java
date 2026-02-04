package sabbir.apk.InterNet.Deta;

import java.time.LocalTime;

public class ScheduleItem {

    public String subject;
    public String instructor;
    public String room;
    public String period;
    public LocalTime start;
    public LocalTime end;
    public ClassState state;

    public boolean isCurrent(LocalTime now) {
        if (start == null || end == null) {
            return false;
        }
        return !now.isBefore(start) && now.isBefore(end);
    }

    public boolean isPast(LocalTime now) {
        if (start == null || end == null) {
            return false;
        }
        return now.isAfter(end);
    }
}
