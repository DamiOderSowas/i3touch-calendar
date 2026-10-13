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
import android.graphics.Rect;
import android.view.MotionEvent;

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
    private float touchX, touchY;
    private boolean trackingSwipe, swiping;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new CalendarDb(this);
        if (state != null) { focus.setTimeInMillis(state.getLong("focus", System.currentTimeMillis())); mode = state.getInt("mode"); }
        root = column(); root.setBackgroundColor(0xfff6f7fa); setContentView(root);
        getWindow().setStatusBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setNavigationBarColor(0xfff6f7fa);
        LinearLayout header = column(); header.setBackgroundColor(WHITE);
        header.setPadding(dp(20), dp(12), dp(20), dp(10)); root.addView(header);
        LinearLayout top = row(); top.setGravity(Gravity.CENTER_VERTICAL); header.addView(top);
        LinearLayout titles = column();
        titles.addView(label("Kalender", 26, INK, true));
        heading = label("", 17, MUTED, false); heading.setSingleLine(true);
        heading.setTag("period-title");
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END); titles.addView(heading);
        top.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        addControl(top, button("Heute", () -> { focus = Calendar.getInstance(); render(); }, false), 100);
        addControl(top, button("+ Termin", () -> edit(null, dayStart(focus).getTimeInMillis()), true), 120);
        LinearLayout bar = row(); bar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams barP = new LinearLayout.LayoutParams(-1, dp(52)); barP.topMargin = dp(10); header.addView(bar, barP);
        monthTab = button("Monat", () -> { mode = 0; render(); }, false); addControl(bar, monthTab, 95);
        weekTab = button("Woche", () -> { mode = 1; render(); }, false); addControl(bar, weekTab, 95);
        dayTab = button("Tag", () -> { mode = 2; render(); }, false); addControl(bar, dayTab, 80);
        View spacer = new View(this); bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        addControl(bar, button("‹", () -> shift(-1), false), 52);
        addControl(bar, button("›", () -> shift(1), false), 52);
        View separator = new View(this); separator.setBackgroundColor(0xffe6e8ee); root.addView(separator, new LinearLayout.LayoutParams(-1, dp(1)));
        content = column(); content.setTag("calendar-content"); root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView hint = label("Nach links oder rechts wischen, um den Zeitraum zu wechseln", 12, MUTED, false);
        hint.setGravity(Gravity.CENTER); hint.setPadding(0, dp(5), 0, dp(5)); root.addView(hint);
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong("focus", focus.getTimeInMillis()); out.putInt("mode", mode); super.onSaveInstanceState(out);
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            Rect area = new Rect(); content.getGlobalVisibleRect(area);
            trackingSwipe = area.contains((int)e.getRawX(), (int)e.getRawY());
            swiping = false; touchX = e.getRawX(); touchY = e.getRawY();
        }
        if (e.getPointerCount() > 1) { trackingSwipe = false; swiping = false; }
        int direction = SwipeNavigation.direction(e.getRawX() - touchX, e.getRawY() - touchY, dp(70));
        if (trackingSwipe && e.getActionMasked() == MotionEvent.ACTION_MOVE && direction != 0 && !swiping) {
            MotionEvent cancel = MotionEvent.obtain(e); cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel); cancel.recycle(); swiping = true;
        }
        if (trackingSwipe && swiping) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                trackingSwipe = false; swiping = false;
                if (direction != 0) shift(direction);
            } else if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) { trackingSwipe = false; swiping = false; }
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) trackingSwipe = false;
        return super.dispatchTouchEvent(e);
    }

    private void addControl(LinearLayout parent, Button b, int width) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(width), dp(46)); p.leftMargin = dp(6); parent.addView(b, p);
    }
    private void styleTab(Button b, boolean selected) {
        b.setTextColor(selected ? RED : MUTED);
        b.setBackground(shape(selected ? 0xffffeded : 0xfff7f8fa, selected ? 0xffffd9dd : 0xfff7f8fa, dp(12)));
    }

    private void shift(int amount) {
        focus.add(mode == 0 ? Calendar.MONTH : mode == 1 ? Calendar.WEEK_OF_YEAR : Calendar.DAY_OF_MONTH, amount);
        render();
        content.setTranslationX(dp(18) * amount);
        content.setAlpha(.7f);
        content.animate().translationX(0).alpha(1).setDuration(180).start();
    }

    private void render() {
        content.removeAllViews();
        styleTab(monthTab, mode == 0);
        styleTab(weekTab, mode == 1);
        styleTab(dayTab, mode == 2);
        heading.setText(mode == 0 ? format("MMMM yyyy", focus.getTime()) :
                mode == 1 ? "Woche · " + format("MMMM yyyy", focus.getTime()) : format("EEEE, d. MMMM yyyy", focus.getTime()));
        if (mode == 0) month(); else if (mode == 1) week(); else day();
    }

    private void month() {
        Calendar first = dayStart(focus); first.set(Calendar.DAY_OF_MONTH, 1);
        Calendar date = (Calendar) first.clone(); date.add(Calendar.DAY_OF_MONTH, -(first.get(Calendar.DAY_OF_WEEK) + 5) % 7);
        Calendar gridEnd = (Calendar) date.clone(); gridEnd.add(Calendar.DAY_OF_MONTH, 42);
        List<CalendarDb.Event> events = db.between(date.getTimeInMillis(), gridEnd.getTimeInMillis());
        LinearLayout grid = column(); grid.setPadding(dp(12), dp(8), dp(12), 0);
        content.addView(grid, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout weekdays = row(); grid.addView(weekdays, new LinearLayout.LayoutParams(-1, dp(30)));
        String[] names = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
        for (String name : names) {
            TextView n = label(name, 14, MUTED, true); n.setGravity(Gravity.CENTER);
            weekdays.addView(n, new LinearLayout.LayoutParams(0, -1, 1));
        }
        for (int r = 0; r < 6; r++) {
            LinearLayout weekRow = row(); grid.addView(weekRow, new LinearLayout.LayoutParams(-1, 0, 1));
            for (int col = 0; col < 7; col++) {
                final long start = date.getTimeInMillis();
                Calendar next = (Calendar) date.clone(); next.add(Calendar.DAY_OF_MONTH, 1); long end = next.getTimeInMillis();
                boolean today = sameDay(date, Calendar.getInstance());
                LinearLayout cell = column(); cell.setPadding(dp(7), dp(3), dp(7), dp(2));
                cell.setBackground(shape(today ? 0xfffff1f2 : WHITE, 0xffe9ebf0, dp(8)));
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1); cp.setMargins(dp(2), dp(2), dp(2), dp(2)); weekRow.addView(cell, cp);
                LinearLayout dateLine = row(); dateLine.setGravity(Gravity.CENTER_VERTICAL); cell.addView(dateLine);
                TextView number = label(Integer.toString(date.get(Calendar.DAY_OF_MONTH)), 16,
                        today ? WHITE : date.get(Calendar.MONTH) != first.get(Calendar.MONTH) ? MUTED : INK, true);
                number.setGravity(Gravity.CENTER);
                if (today) number.setBackground(shape(RED, RED, dp(14)));
                dateLine.addView(number, new LinearLayout.LayoutParams(dp(27), dp(27)));
                if (today) { TextView badge = label("Heute", 11, RED, true); badge.setPadding(dp(4), 0, 0, 0); dateLine.addView(badge); }
                cell.setOnClickListener(v -> { focus.setTimeInMillis(start); mode = 2; render(); });
                int total = 0;
                for (CalendarDb.Event event : events) if (event.start < end && event.end > start) {
                    total++;
                    if (total <= 2) {
                        TextView chip = label(event.title, 11, event.color, true);
                        chip.setSingleLine(true); chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        chip.setPadding(dp(2), 0, 0, 0); cell.addView(chip);
                        chip.setOnClickListener(v -> edit(event, start));
                    }
                }
                if (total > 2) cell.addView(label("+" + (total - 2) + " weitere", 10, MUTED, false));
                date = next;
            }
        }
    }

    private void week() {
        Calendar start = dayStart(focus); start.add(Calendar.DAY_OF_MONTH, -(start.get(Calendar.DAY_OF_WEEK) + 5) % 7);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_MONTH, 7);
        List<CalendarDb.Event> events = db.between(start.getTimeInMillis(), end.getTimeInMillis());
        LinearLayout days = row(); days.setPadding(dp(12), dp(12), dp(12), 0);
        content.addView(days, new LinearLayout.LayoutParams(-1, -1));
        for (int i = 0; i < 7; i++) {
            final long from = start.getTimeInMillis();
            Calendar next = (Calendar) start.clone(); next.add(Calendar.DAY_OF_MONTH, 1); long to = next.getTimeInMillis();
            boolean today = sameDay(start, Calendar.getInstance());
            LinearLayout panel = column(); panel.setPadding(dp(7), dp(8), dp(7), dp(8));
            panel.setTag("week-day");
            panel.setBackground(shape(today ? 0xfffff1f2 : WHITE, 0xffe6e8ee, dp(12)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1); p.setMargins(dp(3), 0, dp(3), 0); days.addView(panel, p);
            TextView title = label(format("EEE, d.", new Date(from)), 16, today ? RED : INK, true); panel.addView(title);
            if (today) panel.addView(label("Heute", 12, RED, true));
            title.setOnClickListener(v -> { focus.setTimeInMillis(from); mode = 2; render(); });
            ScrollView scroll = new ScrollView(this); panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            LinearLayout list = column(); scroll.addView(list);
            for (CalendarDb.Event event : events) if (event.start < to && event.end > from) addEvent(list, event);
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
        card.setBackground(shape(WHITE, 0xffe8eaf0, dp(12)));
        card.setElevation(dp(1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(10); parent.addView(card, p);
        card.addView(label(event.title, mode == 1 ? 13 : 20, event.color, true));
        String time = event.allDay ? "Ganztägig" : format("HH:mm", new Date(event.start)) + " – " + format("HH:mm", new Date(event.end));
        card.addView(label(time, mode == 1 ? 11 : 15, MUTED, false));
        if (!event.notes.isEmpty() && mode != 1) card.addView(label(event.notes, 14, MUTED, false));
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
        b.setPadding(dp(10), 0, dp(10), 0); b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(dp(48));
        if (action != null) b.setOnClickListener(v -> action.run()); return b;
    }
    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(radius); d.setStroke(dp(1), stroke); return d;
    }
}
