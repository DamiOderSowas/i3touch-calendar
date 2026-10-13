package de.aufgekaut.i3kalender;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int WHITE = Color.WHITE, INK = 0xff1c1c1e, MUTED = 0xff8e8e93;
    private static final int RED = 0xffe3434f, BLUE = 0xff3a84e8, GREEN = 0xff36a881, ORANGE = 0xffed9a36;
    private static final int[] COLORS = {RED, BLUE, GREEN, ORANGE};
    private final Locale locale = Locale.GERMANY;
    private Calendar focus = Calendar.getInstance();
    private CalendarDb db;
    private LinearLayout root, content;
    private TextView heading;
    private Button monthTab, weekTab, dayTab;
    private int mode = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new CalendarDb(this);
        if (state != null) { focus.setTimeInMillis(state.getLong("focus", System.currentTimeMillis())); mode = state.getInt("mode"); }
        root = column(); root.setBackgroundColor(WHITE); setContentView(root);
        LinearLayout bar = row(); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(24), dp(15), dp(24), dp(15)); root.addView(bar);
        TextView logo = label("Kalender", 25, RED, true); bar.addView(logo);
        heading = label("", 23, INK, true);
        LinearLayout.LayoutParams headP = new LinearLayout.LayoutParams(0, -2, 1); headP.leftMargin = dp(38); bar.addView(heading, headP);
        bar.addView(button("‹", () -> shift(-1), false));
        bar.addView(button("Heute", () -> { focus = Calendar.getInstance(); render(); }, false));
        bar.addView(button("›", () -> shift(1), false));
        monthTab = button("Monat", () -> { mode = 0; render(); }, false); bar.addView(monthTab);
        weekTab = button("Woche", () -> { mode = 1; render(); }, false); bar.addView(weekTab);
        dayTab = button("Tag", () -> { mode = 2; render(); }, false); bar.addView(dayTab);
        bar.addView(button("+ Termin", () -> edit(null, dayStart(focus).getTimeInMillis()), true));
        View separator = new View(this); separator.setBackgroundColor(0xffe4e4e7); root.addView(separator, new LinearLayout.LayoutParams(-1, dp(1)));
        content = column(); root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong("focus", focus.getTimeInMillis()); out.putInt("mode", mode); super.onSaveInstanceState(out);
    }

    private void shift(int amount) {
        focus.add(mode == 0 ? Calendar.MONTH : mode == 1 ? Calendar.WEEK_OF_YEAR : Calendar.DAY_OF_MONTH, amount);
        render();
    }

    private void render() {
        content.removeAllViews();
        monthTab.setTextColor(mode == 0 ? RED : INK);
        weekTab.setTextColor(mode == 1 ? RED : INK);
        dayTab.setTextColor(mode == 2 ? RED : INK);
        heading.setText(mode == 0 ? format("MMMM yyyy", focus.getTime()) :
                mode == 1 ? "Woche · " + format("MMMM yyyy", focus.getTime()) : format("EEEE, d. MMMM yyyy", focus.getTime()));
        if (mode == 0) month(); else if (mode == 1) week(); else day();
    }

    private void month() {
        Calendar first = dayStart(focus); first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        Calendar gridStart = (Calendar) first.clone(); gridStart.add(Calendar.DAY_OF_MONTH, -offset);
        Calendar gridEnd = (Calendar) gridStart.clone(); gridEnd.add(Calendar.DAY_OF_MONTH, 42);
        List<CalendarDb.Event> events = db.between(gridStart.getTimeInMillis(), gridEnd.getTimeInMillis());
        ScrollView scroll = new ScrollView(this); content.addView(scroll);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(7); grid.setPadding(dp(16), dp(8), dp(16), dp(16)); scroll.addView(grid);
        String[] names = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
        int width = (getResources().getDisplayMetrics().widthPixels - dp(32)) / 7;
        for (String name : names) { TextView n = label(name, 15, MUTED, true); n.setGravity(Gravity.CENTER); grid.addView(n, new GridLayout.LayoutParams(new android.view.ViewGroup.LayoutParams(width, dp(36)))); }
        Calendar date = (Calendar) gridStart.clone();
        for (int i = 0; i < 42; i++) {
            final long start = date.getTimeInMillis();
            Calendar next = (Calendar) date.clone(); next.add(Calendar.DAY_OF_MONTH, 1);
            long end = next.getTimeInMillis();
            LinearLayout cell = column(); cell.setPadding(dp(8), dp(5), dp(8), dp(3));
            GradientDrawable border = shape(WHITE, 0xffededf0, 0); cell.setBackground(border);
            int numberColor = date.get(Calendar.MONTH) != first.get(Calendar.MONTH) ? MUTED : INK;
            if (sameDay(date, Calendar.getInstance())) numberColor = RED;
            TextView number = label(Integer.toString(date.get(Calendar.DAY_OF_MONTH)), 17, numberColor, true);
            cell.addView(number); cell.setOnClickListener(v -> { focus.setTimeInMillis(start); mode = 2; render(); });
            int shown = 0;
            for (CalendarDb.Event event : events) {
                if (event.start < end && event.end > start) {
                    if (shown++ >= 3) break;
                    TextView chip = label((event.allDay ? "" : format("HH:mm ", new Date(event.start))) + event.title, 12, event.color, false);
                    chip.setSingleLine(true); chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    chip.setPadding(dp(3), dp(3), dp(2), dp(3)); cell.addView(chip);
                    chip.setOnClickListener(v -> edit(event, start));
                }
            }
            if (shown > 3) cell.addView(label("+ weitere", 11, MUTED, false));
            grid.addView(cell, new GridLayout.LayoutParams(new android.view.ViewGroup.LayoutParams(width, dp(100))));
            date = next;
        }
    }

    private void week() {
        Calendar start = dayStart(focus); start.add(Calendar.DAY_OF_MONTH, -(start.get(Calendar.DAY_OF_WEEK) + 5) % 7);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_MONTH, 7);
        List<CalendarDb.Event> events = db.between(start.getTimeInMillis(), end.getTimeInMillis());
        HorizontalScrollView horizontal = new HorizontalScrollView(this); content.addView(horizontal);
        LinearLayout days = row(); days.setPadding(dp(16), dp(16), dp(16), dp(16)); horizontal.addView(days);
        for (int i = 0; i < 7; i++) {
            final long from = start.getTimeInMillis();
            Calendar next = (Calendar) start.clone(); next.add(Calendar.DAY_OF_MONTH, 1); long to = next.getTimeInMillis();
            LinearLayout panel = column(); panel.setPadding(dp(10), dp(8), dp(10), dp(8));
            panel.setBackground(shape(0xfff8f8fa, 0xffebebef, dp(12)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(180), dp(480)); p.rightMargin = dp(8); days.addView(panel, p);
            panel.addView(label(format("EEE, d. MMM", new Date(from)), 16, sameDay(start, Calendar.getInstance()) ? RED : INK, true));
            panel.setOnClickListener(v -> { focus.setTimeInMillis(from); mode = 2; render(); });
            for (CalendarDb.Event event : events) if (event.start < to && event.end > from) addEvent(panel, event);
            start = next;
        }
    }

    private void day() {
        Calendar from = dayStart(focus); Calendar to = (Calendar) from.clone(); to.add(Calendar.DAY_OF_MONTH, 1);
        List<CalendarDb.Event> events = db.between(from.getTimeInMillis(), to.getTimeInMillis());
        ScrollView scroll = new ScrollView(this); content.addView(scroll);
        LinearLayout list = column(); list.setPadding(dp(24), dp(18), dp(24), dp(18)); scroll.addView(list);
        if (events.isEmpty()) list.addView(label("Keine Termine · Tippe auf + Termin", 19, MUTED, false));
        for (CalendarDb.Event event : events) addEvent(list, event);
    }

    private void addEvent(LinearLayout parent, CalendarDb.Event event) {
        LinearLayout card = column(); card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(shape(0xfffafafa, event.color, dp(10)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(10); parent.addView(card, p);
        card.addView(label(event.title, 17, INK, true));
        String time = event.allDay ? "Ganztägig" : format("HH:mm", new Date(event.start)) + " – " + format("HH:mm", new Date(event.end));
        card.addView(label(time, 14, MUTED, false));
        if (!event.notes.isEmpty()) card.addView(label(event.notes, 13, MUTED, false));
        card.setOnClickListener(v -> edit(event, event.start));
    }

    private void edit(CalendarDb.Event existing, long selectedDay) {
        CalendarDb.Event event = existing == null ? new CalendarDb.Event() : existing;
        if (existing == null) {
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedDay); c.set(Calendar.HOUR_OF_DAY, 9);
            event.start = c.getTimeInMillis(); c.add(Calendar.HOUR_OF_DAY, 1); event.end = c.getTimeInMillis(); event.color = RED;
            event.title = ""; event.notes = "";
        }
        final Calendar start = Calendar.getInstance(), end = Calendar.getInstance();
        start.setTimeInMillis(event.start); end.setTimeInMillis(event.end);
        // Ganztägige Termine werden mit exklusivem Ende gespeichert;
        // im Editor wird der letzte eingeschlossene Kalendertag angezeigt.
        if (event.allDay) end.add(Calendar.DAY_OF_MONTH, -1);
        final int[] chosenColor = {event.color};
        ScrollView scroller = new ScrollView(this);
        LinearLayout form = column(); form.setPadding(dp(24), dp(12), dp(24), dp(8)); scroller.addView(form);
        EditText title = new EditText(this); title.setSingleLine(true); title.setText(event.title); title.setHint("Titel"); form.addView(title);
        CheckBox allDay = new CheckBox(this); allDay.setText("Ganztägig"); allDay.setChecked(event.allDay); form.addView(allDay);
        Button startDate = button("", null, false), startTime = button("", null, false);
        Button endDate = button("", null, false), endTime = button("", null, false);
        Runnable refresh = () -> {
            startDate.setText("Beginn: " + format("dd.MM.yyyy", start.getTime())); startTime.setText(format("HH:mm", start.getTime()));
            endDate.setText("Ende: " + format("dd.MM.yyyy", end.getTime())); endTime.setText(format("HH:mm", end.getTime()));
            startTime.setVisibility(allDay.isChecked() ? View.GONE : View.VISIBLE);
            endTime.setVisibility(allDay.isChecked() ? View.GONE : View.VISIBLE);
        };
        form.addView(pair(startDate, startTime)); form.addView(pair(endDate, endTime));
        startDate.setOnClickListener(v -> pickDate(start, refresh)); endDate.setOnClickListener(v -> pickDate(end, refresh));
        startTime.setOnClickListener(v -> pickTime(start, refresh)); endTime.setOnClickListener(v -> pickTime(end, refresh));
        allDay.setOnCheckedChangeListener((v, checked) -> refresh.run()); refresh.run();
        EditText notes = new EditText(this); notes.setHint("Notiz (optional)"); notes.setMinLines(2); notes.setText(event.notes); form.addView(notes);
        form.addView(label("Farbe", 15, MUTED, true));
        LinearLayout swatches = row(); form.addView(swatches);
        for (int color : COLORS) {
            Button swatch = button("●", () -> { chosenColor[0] = color; Toast.makeText(this, "Farbe gewählt", Toast.LENGTH_SHORT).show(); }, false);
            swatch.setTextColor(color); swatches.addView(swatch);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "Neuer Termin" : "Termin bearbeiten")
                .setView(scroller).setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", null)
                .setNeutralButton(existing == null ? null : "Löschen", (d, w) -> {
                    new AlertDialog.Builder(this).setMessage("Termin wirklich löschen?")
                            .setNegativeButton("Abbrechen", null)
                            .setPositiveButton("Löschen", (confirm, which) -> { db.remove(event.id); render(); }).show();
                }).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(save -> {
            String name = title.getText().toString().trim();
            if (name.isEmpty()) { title.setError("Bitte einen Titel eingeben"); return; }
            long startMs = start.getTimeInMillis(), endMs = end.getTimeInMillis();
            if (allDay.isChecked()) {
                startMs = dayStart(start).getTimeInMillis();
                Calendar exclusive = dayStart(end); exclusive.add(Calendar.DAY_OF_MONTH, 1); endMs = exclusive.getTimeInMillis();
            }
            if (endMs <= startMs) { Toast.makeText(this, "Ende muss nach dem Beginn liegen", Toast.LENGTH_LONG).show(); return; }
            event.title = name; event.notes = notes.getText().toString().trim();
            event.start = startMs; event.end = endMs; event.allDay = allDay.isChecked(); event.color = chosenColor[0];
            db.save(event); focus.setTimeInMillis(startMs); dialog.dismiss(); render();
        }));
        dialog.show();
    }

    private void pickDate(Calendar c, Runnable done) {
        new DatePickerDialog(this, (picker, year, month, day) -> { c.set(year, month, day); done.run(); },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void pickTime(Calendar c, Runnable done) {
        new TimePickerDialog(this, (picker, hour, minute) -> { c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, minute); done.run(); },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }
    private LinearLayout pair(View a, View b) {
        LinearLayout line = row(); line.addView(a, new LinearLayout.LayoutParams(0, -2, 2));
        line.addView(b, new LinearLayout.LayoutParams(0, -2, 1)); return line;
    }
    private Calendar dayStart(Calendar c) {
        Calendar copy = (Calendar) c.clone(); copy.set(Calendar.HOUR_OF_DAY, 0); copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0); copy.set(Calendar.MILLISECOND, 0); return copy;
    }
    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
    private String format(String pattern, Date date) { return new SimpleDateFormat(pattern, locale).format(date); }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private Button button(String title, Runnable action, boolean accent) {
        Button b = new Button(this); b.setText(title); b.setTextSize(14); b.setAllCaps(false);
        b.setTextColor(accent ? WHITE : INK); b.setBackground(shape(accent ? RED : 0xfff4f4f6, 0xffe9e9eb, dp(10)));
        b.setPadding(dp(12), 0, dp(12), 0); b.setMinHeight(dp(48));
        if (action != null) b.setOnClickListener(v -> action.run()); return b;
    }
    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(radius); d.setStroke(dp(1), stroke); return d;
    }
}
