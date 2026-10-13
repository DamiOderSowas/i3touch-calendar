package de.aufgekaut.i3kalender;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class CalendarDb extends SQLiteOpenHelper {
    static final class Event {
        long id, start, end;
        String title, notes;
        int color;
        boolean allDay;
    }

    CalendarDb(Context context) { super(context, "calendar.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '', start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, all_day INTEGER NOT NULL DEFAULT 0, color INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX events_time ON events(start_ms,end_ms)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    List<Event> between(long start, long end) {
        List<Event> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("events", null,
                "start_ms < ? AND end_ms > ?", new String[]{Long.toString(end), Long.toString(start)},
                null, null, "start_ms ASC, id ASC")) {
            while (c.moveToNext()) {
                Event e = new Event();
                e.id = c.getLong(c.getColumnIndexOrThrow("id"));
                e.title = c.getString(c.getColumnIndexOrThrow("title"));
                e.notes = c.getString(c.getColumnIndexOrThrow("notes"));
                e.start = c.getLong(c.getColumnIndexOrThrow("start_ms"));
                e.end = c.getLong(c.getColumnIndexOrThrow("end_ms"));
                e.allDay = c.getInt(c.getColumnIndexOrThrow("all_day")) != 0;
                e.color = c.getInt(c.getColumnIndexOrThrow("color"));
                out.add(e);
            }
        }
        return out;
    }

    void save(Event e) {
        ContentValues v = new ContentValues();
        v.put("title", e.title); v.put("notes", e.notes);
        v.put("start_ms", e.start); v.put("end_ms", e.end);
        v.put("all_day", e.allDay ? 1 : 0); v.put("color", e.color);
        if (e.id == 0) e.id = getWritableDatabase().insertOrThrow("events", null, v);
        else getWritableDatabase().update("events", v, "id=?", new String[]{Long.toString(e.id)});
    }

    void remove(long id) { getWritableDatabase().delete("events", "id=?", new String[]{Long.toString(id)}); }
}
