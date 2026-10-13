package de.aufgekaut.i3kalender;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CalendarUiTest {
    private Instrumentation inst;
    private MainActivity activity;
    @Before public void open() {
        inst = InstrumentationRegistry.getInstrumentation();
        Intent i = new Intent(inst.getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = (MainActivity) inst.startActivitySync(i);
        inst.waitForIdleSync();
    }
    @After public void close() { inst.runOnMainSync(() -> activity.finish()); }

    @Test public void todayButtonIsVisibleAndCurrentDayIsMarked() {
        inst.runOnMainSync(() -> {
            Button today = button(activity.getWindow().getDecorView(), "Heute");
            assertNotNull(today);
            Rect visible = new Rect(); assertTrue(today.getGlobalVisibleRect(visible));
            assertEquals(today.getWidth(), visible.width());
            assertEquals(today.getHeight(), visible.height());
            assertTrue(countText(activity.getWindow().getDecorView(), "Heute") >= 2);
            View number = activity.getWindow().getDecorView().findViewWithTag("today-number");
            assertNotNull(number); Rect numberRect = new Rect();
            assertTrue(number.getGlobalVisibleRect(numberRect));
            assertEquals(number.getHeight(), numberRect.height());
            TextView badge = activity.getWindow().getDecorView().findViewWithTag("today-badge");
            assertNotNull(badge); assertEquals(1, badge.getLineCount());
            assertTrue(badge.getPaint().measureText("Heute") <= badge.getWidth() - badge.getPaddingLeft() - badge.getPaddingRight());
        });
    }

    @Test public void swipeChangesMonthAndTodayReturnsToCurrentMonth() {
        String before = heading(); swipe(-1); assertNotEquals(before, heading());
        inst.runOnMainSync(() -> button(activity.getWindow().getDecorView(), "Heute").performClick());
        inst.waitForIdleSync();
        assertEquals(new SimpleDateFormat("MMMM yyyy", Locale.GERMANY).format(new Date()), heading());
        swipe(1); assertNotEquals(before, heading());
    }

    @Test public void allSevenWeekColumnsFitAndSwipeChangesWeek() {
        inst.runOnMainSync(() -> button(activity.getWindow().getDecorView(), "Woche").performClick());
        inst.waitForIdleSync();
        inst.runOnMainSync(() -> {
            List<View> days = new ArrayList<>(); collect(activity.getWindow().getDecorView(), "week-day", days);
            assertEquals(7, days.size());
            for (View day : days) {
                Rect r = new Rect(); assertTrue(day.getGlobalVisibleRect(r)); assertEquals(day.getWidth(), r.width());
            }
        });
        String first = firstWeekDay(); swipe(-1); assertNotEquals(first, firstWeekDay());
    }

    private String heading() {
        final String[] result = {null};
        inst.runOnMainSync(() -> result[0] = ((TextView)activity.getWindow().getDecorView().findViewWithTag("period-title")).getText().toString());
        return result[0];
    }
    private String firstWeekDay() {
        final String[] result = {null};
        inst.runOnMainSync(() -> {
            List<View> days = new ArrayList<>(); collect(activity.getWindow().getDecorView(), "week-day", days);
            result[0] = ((TextView)((ViewGroup)days.get(0)).getChildAt(0)).getText().toString();
        });
        return result[0];
    }
    private void swipe(int sign) {
        Rect r = new Rect(); inst.runOnMainSync(() -> activity.getWindow().getDecorView().findViewWithTag("calendar-content").getGlobalVisibleRect(r));
        float start = r.left + r.width() * (sign < 0 ? .75f : .25f);
        float end = r.left + r.width() * (sign < 0 ? .25f : .75f);
        float y = r.top + r.height() * .6f; long now = SystemClock.uptimeMillis();
        send(now, now, MotionEvent.ACTION_DOWN, start, y);
        for (int i = 1; i <= 10; i++) {
            SystemClock.sleep(20); send(now, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, start + (end-start)*i/10, y);
        }
        send(now, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, end, y); inst.waitForIdleSync();
    }
    private void send(long down, long time, int action, float x, float y) {
        MotionEvent e = MotionEvent.obtain(down, time, action, x, y, 0); inst.sendPointerSync(e); e.recycle();
    }
    private Button button(View root, String text) {
        if (root instanceof Button && ((Button)root).getText().toString().equals(text)) return (Button)root;
        if (root instanceof ViewGroup) for (int i=0;i<((ViewGroup)root).getChildCount();i++) {
            Button b=button(((ViewGroup)root).getChildAt(i),text); if(b!=null)return b;
        }
        return null;
    }
    private int countText(View root, String text) {
        int n=root instanceof TextView && ((TextView)root).getText().toString().equals(text)?1:0;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)n+=countText(((ViewGroup)root).getChildAt(i),text);
        return n;
    }
    private void collect(View root, String tag, List<View> out) {
        if(tag.equals(root.getTag()))out.add(root);
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)collect(((ViewGroup)root).getChildAt(i),tag,out);
    }
}
