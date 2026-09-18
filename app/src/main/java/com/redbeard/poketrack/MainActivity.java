package com.redbeard.poketrack;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "poketrack_progress";
    private static final String WATCHED = "watched_ids";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ExpandableListView list;
    private WatchAdapter adapter;
    private TextView countText;
    private TextView percentText;
    private TextView nextTitle;
    private TextView nextMeta;
    private ProgressBar progress;
    private Button syncButton;

    private List<CatalogGroup> groups = new ArrayList<>();
    private Set<String> watched = new HashSet<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        watched = new HashSet<>(prefs.getStringSet(WATCHED, Collections.emptySet()));
        groups = CatalogRepository.seed();

        list = new ExpandableListView(this);
        list.setGroupIndicator(null);
        list.setDivider(null);
        list.setChildDivider(null);
        list.setBackgroundColor(Color.rgb(248, 242, 216));
        list.setPadding(dp(10), dp(10), dp(10), dp(18));
        list.setClipToPadding(false);
        list.addHeaderView(buildHeader(), null, false);

        adapter = new WatchAdapter();
        list.setAdapter(adapter);
        setContentView(list);

        updateProgress();
        goToNext(false);
        sync(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private View buildHeader() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(2), dp(2), dp(2), dp(14));

        LinearLayout hero = card(Color.rgb(255, 252, 238), Color.rgb(190, 153, 42), 3, 18);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView title = text("Pokétrack", 24, Color.rgb(42, 42, 42), true);
        title.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        hero.addView(title);

        TextView sub = text("Cronologia completa · serie, film e speciali", 13, Color.rgb(92, 88, 70), false);
        hero.addView(sub, verticalParams(dp(3), dp(8)));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        countText = text("0 / 0 visti", 13, Color.rgb(72, 69, 55), true);
        stats.addView(countText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        percentText = text("0%", 22, Color.rgb(61, 116, 72), true);
        stats.addView(percentText);
        hero.addView(stats);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(ColorStateList.valueOf(Color.rgb(90, 165, 108)));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(226, 218, 183)));
        hero.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(12)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button continueButton = button("Continua da qui");
        continueButton.setOnClickListener(v -> goToNext(true));
        actions.addView(continueButton, new LinearLayout.LayoutParams(0, dp(48), 1));

        View gap = new View(this);
        actions.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));

        syncButton = button("Aggiorna catalogo");
        syncButton.setOnClickListener(v -> sync(true));
        actions.addView(syncButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        hero.addView(actions, verticalParams(dp(12), 0));

        wrap.addView(hero, verticalParams(0, dp(10)));

        LinearLayout nextCard = card(Color.rgb(255, 250, 226), Color.rgb(202, 167, 57), 2, 16);
        nextCard.setOrientation(LinearLayout.VERTICAL);
        nextCard.setPadding(dp(15), dp(12), dp(15), dp(12));

        nextCard.addView(text("CONTINUA DA QUI", 11, Color.rgb(120, 103, 47), true));
        nextTitle = text("", 18, Color.rgb(45, 45, 45), true);
        nextTitle.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        nextCard.addView(nextTitle, verticalParams(dp(3), 0));

        nextMeta = text("", 12, Color.rgb(100, 95, 78), false);
        nextCard.addView(nextMeta, verticalParams(dp(3), 0));

        wrap.addView(nextCard);
        return wrap;
    }

    private void sync(boolean manual) {
        if (syncButton == null) return;
        syncButton.setEnabled(false);
        syncButton.setText("Aggiornamento…");

        executor.execute(() -> {
            try {
                List<CatalogGroup> fresh = CatalogRepository.download();
                main.post(() -> {
                    groups = fresh;
                    adapter.notifyDataSetChanged();
                    updateProgress();
                    goToNext(false);
                    syncButton.setEnabled(true);
                    syncButton.setText("Aggiorna catalogo");
                    if (manual) {
                        Toast.makeText(this, "Catalogo aggiornato", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                main.post(() -> {
                    syncButton.setEnabled(true);
                    syncButton.setText("Aggiorna catalogo");
                    if (manual) {
                        Toast.makeText(this, "Aggiornamento non riuscito. Il progresso resta salvato.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void toggle(ContentItem item, boolean checked) {
        if (checked) watched.add(item.id);
        else watched.remove(item.id);
        prefs.edit().putStringSet(WATCHED, new HashSet<>(watched)).apply();
        updateProgress();
        adapter.notifyDataSetChanged();
    }

    private void updateProgress() {
        int total = 0;
        int done = 0;
        for (CatalogGroup g : groups) {
            for (ContentItem i : g.items) {
                total++;
                if (watched.contains(i.id)) done++;
            }
        }

        int p = total == 0 ? 0 : Math.round(done * 1000f / total);
        progress.setProgress(p);
        countText.setText(done + " / " + total + " visti");
        float pct = p / 10f;
        percentText.setText(pct == Math.round(pct)
                ? String.format(Locale.ITALY, "%.0f%%", pct)
                : String.format(Locale.ITALY, "%.1f%%", pct));

        int[] next = findNext();
        if (next == null) {
            nextTitle.setText("Catalogo completato");
            nextMeta.setText("Hai visto tutti i contenuti presenti.");
        } else {
            CatalogGroup g = groups.get(next[0]);
            ContentItem i = g.items.get(next[1]);
            nextTitle.setText(i.title);
            nextMeta.setText(g.title + " · " + i.subtitle());
        }
    }

    private int[] findNext() {
        for (int g = 0; g < groups.size(); g++) {
            for (int c = 0; c < groups.get(g).items.size(); c++) {
                if (!watched.contains(groups.get(g).items.get(c).id)) return new int[]{g, c};
            }
        }
        return null;
    }

    private void goToNext(boolean scroll) {
        list.postDelayed(() -> {
            int[] next = findNext();
            if (next == null) return;
            for (int i = 0; i < groups.size(); i++) {
                if (i != next[0]) list.collapseGroup(i);
            }
            list.expandGroup(next[0], true);
            if (scroll) list.setSelectedChild(next[0], next[1], true);
        }, 100);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.rgb(48, 48, 48));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(246, 214, 82));
        bg.setStroke(dp(1), Color.rgb(151, 119, 28));
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        return b;
    }

    private LinearLayout card(int fill, int stroke, int strokeWidth, int radius) {
        LinearLayout l = new LinearLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setStroke(dp(strokeWidth), stroke);
        bg.setCornerRadius(dp(radius));
        l.setBackground(bg);
        return l;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams verticalParams(int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private class WatchAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return groups.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return groups.get(groupPosition).items.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groups.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return groups.get(groupPosition).items.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return groupPosition * 10000L + childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public View getGroupView(int position, boolean expanded, View convertView, ViewGroup parent) {
            CatalogGroup g = groups.get(position);

            LinearLayout box = card(Color.rgb(247, 211, 70), Color.rgb(148, 113, 24), 2, 16);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(12), dp(10), dp(12), dp(10));

            LinearLayout top = new LinearLayout(MainActivity.this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            TextView title = text(g.title, 17, Color.rgb(45, 43, 32), true);
            title.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView badge = badge(g.type, Color.rgb(43, 92, 134));
            top.addView(badge);

            TextView arrow = text(expanded ? "  ▲" : "  ▼", 15, Color.rgb(69, 57, 26), true);
            top.addView(arrow);
            box.addView(top);

            int done = 0;
            for (ContentItem i : g.items) if (watched.contains(i.id)) done++;
            String meta;
            if (g.items.isEmpty()) meta = "In attesa dell'aggiornamento catalogo";
            else meta = done + " / " + g.items.size() + " visti · " +
                    Math.round(done * 100f / g.items.size()) + "%";

            box.addView(text(meta, 12, Color.rgb(89, 76, 39), false), verticalParams(dp(4), 0));

            LinearLayout outer = new LinearLayout(MainActivity.this);
            outer.setPadding(dp(2), dp(6), dp(2), dp(4));
            outer.addView(box, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return outer;
        }

        @Override
        public View getChildView(int gp, int cp, boolean lastChild, View convertView, ViewGroup parent) {
            ContentItem item = groups.get(gp).items.get(cp);

            int fill = Color.rgb(255, 253, 244);
            int stroke = Color.rgb(201, 190, 145);
            int badgeColor = Color.rgb(43, 92, 134);

            if ("FILM".equals(item.type)) {
                fill = Color.rgb(255, 241, 225);
                stroke = Color.rgb(181, 91, 53);
                badgeColor = Color.rgb(173, 75, 38);
            } else if ("SPECIALE".equals(item.type)) {
                fill = Color.rgb(247, 240, 255);
                stroke = Color.rgb(115, 84, 153);
                badgeColor = Color.rgb(103, 70, 146);
            } else if ("MINISERIE".equals(item.type)) {
                fill = Color.rgb(237, 249, 251);
                stroke = Color.rgb(77, 122, 139);
                badgeColor = Color.rgb(55, 105, 122);
            }

            LinearLayout row = card(fill, stroke, 1, 13);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(9), dp(8), dp(9), dp(8));

            CheckBox check = new CheckBox(MainActivity.this);
            check.setButtonTintList(ColorStateList.valueOf(Color.rgb(80, 139, 93)));
            check.setChecked(watched.contains(item.id));
            check.setOnCheckedChangeListener((buttonView, isChecked) -> toggle(item, isChecked));
            row.addView(check, new LinearLayout.LayoutParams(dp(44), dp(44)));

            LinearLayout content = new LinearLayout(MainActivity.this);
            content.setOrientation(LinearLayout.VERTICAL);

            LinearLayout titleRow = new LinearLayout(MainActivity.this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView title = text(item.number() + " · " + item.title, 14, Color.rgb(48, 48, 45), true);
            titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (!"EPISODIO".equals(item.type)) titleRow.addView(badge(item.type, badgeColor));

            content.addView(titleRow);
            content.addView(text(item.subtitle(), 11, Color.rgb(105, 100, 82), false), verticalParams(dp(3), 0));

            row.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            LinearLayout outer = new LinearLayout(MainActivity.this);
            outer.setPadding(dp(14), dp(3), dp(2), lastChild ? dp(8) : dp(3));
            outer.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return outer;
        }

        private TextView badge(String label, int color) {
            TextView b = text(label, 10, color, true);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(8), dp(3), dp(8), dp(3));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)));
            bg.setStroke(dp(1), color);
            bg.setCornerRadius(dp(30));
            b.setBackground(bg);
            return b;
        }
    }
}
