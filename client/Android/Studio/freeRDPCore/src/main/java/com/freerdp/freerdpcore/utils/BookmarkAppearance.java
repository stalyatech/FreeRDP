/*
   Per-bookmark tile icon and color for the connection list.

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
   If a copy of the MPL was not distributed with this file, You can obtain one at
   http://mozilla.org/MPL/2.0/.
*/

package com.freerdp.freerdpcore.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.freerdp.freerdpcore.R;
import com.freerdp.freerdpcore.domain.BookmarkBase;

import java.util.Locale;

// Icon and color are kept in SharedPreferences (keyed by bookmark id), not in the
// bookmark database, so the database schema stays identical to upstream.
public final class BookmarkAppearance
{
	public static final String[] ICON_KEYS = { "desktop", "laptop", "server", "terminal",
		                                       "home",    "work",   "cloud",  "game" };
	private static final int[] ICON_RES = { R.drawable.ic_bm_desktop,  R.drawable.ic_bm_laptop,
		                                    R.drawable.ic_bm_server,   R.drawable.ic_bm_terminal,
		                                    R.drawable.ic_bm_home,     R.drawable.ic_bm_work,
		                                    R.drawable.ic_bm_cloud,    R.drawable.ic_bm_game };
	public static final int[] COLORS = { 0xFF1E88E5, 0xFF00897B, 0xFF43A047, 0xFFF4511E,
		                                 0xFF8E24AA, 0xFFD81B60, 0xFF3949AB, 0xFF546E7A };

	private static final String PREFS = "bookmark_appearance";
	private static final String KEY_GRID = "view_grid";

	// Name keywords -> icon index (checked in this order, lower-case, label + host).
	// Keywords of up to 3 letters must match a whole word, longer ones may be part of one.
	private static final String[][] KEYWORDS = {
		{ "server", "sunucu", "srv", "nas", "vps", "proxmox" }, // 2 server
		{ "laptop", "notebook", "dizüstü", "dizustu", "rog", "book" }, // 1 laptop
		{ "terminal", "ssh", "shell", "konsol", "console" },          // 3 terminal
		{ "home", "ev", "evim", "salon" },                             // 4 home
		{ "work", "ofis", "office", "iş", "işyeri", "şirket", "corp" },// 5 work
		{ "cloud", "bulut", "azure", "aws", "gcp" },                   // 6 cloud
		{ "game", "oyun", "gaming" },                                  // 7 game
	};
	private static final int[] KEYWORD_ICON = { 2, 1, 3, 4, 5, 6, 7 };

	private BookmarkAppearance()
	{
	}

	private static SharedPreferences prefs(Context c)
	{
		return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}

	// ---- view mode -----------------------------------------------------------------

	public static boolean isGridView(Context c)
	{
		return prefs(c).getBoolean(KEY_GRID, false);
	}

	public static void setGridView(Context c, boolean grid)
	{
		prefs(c).edit().putBoolean(KEY_GRID, grid).apply();
	}

	// ---- icon / color --------------------------------------------------------------

	public static int autoIconIndex(BookmarkBase b)
	{
		final String t = (b.getLabel() + " " + b.getHostname()).toLowerCase(Locale.ROOT);
		final String words = " " + t.replaceAll("[^\\p{L}\\p{N}]+", " ") + " ";
		for (int i = 0; i < KEYWORDS.length; i++)
			for (String k : KEYWORDS[i])
				if (k.length() <= 3 ? words.contains(" " + k + " ") : t.contains(k))
					return KEYWORD_ICON[i];
		return 0;
	}

	public static int autoColorIndex(BookmarkBase b)
	{
		return Math.abs((b.getLabel() + "|" + b.getHostname()).hashCode()) % COLORS.length;
	}

	// Stored choice, or -1 for automatic.
	public static int storedIconIndex(Context c, long id)
	{
		String key = prefs(c).getString("icon_" + id, null);
		if (key == null)
			return -1;
		for (int i = 0; i < ICON_KEYS.length; i++)
			if (ICON_KEYS[i].equals(key))
				return i;
		return -1;
	}

	public static int storedColorIndex(Context c, long id)
	{
		int v = prefs(c).getInt("color_" + id, -1);
		return (v >= 0 && v < COLORS.length) ? v : -1;
	}

	public static int iconIndex(Context c, BookmarkBase b)
	{
		int i = storedIconIndex(c, b.getId());
		return i >= 0 ? i : autoIconIndex(b);
	}

	public static int colorIndex(Context c, BookmarkBase b)
	{
		int i = storedColorIndex(c, b.getId());
		return i >= 0 ? i : autoColorIndex(b);
	}

	public static void store(Context c, long id, int iconIndex, int colorIndex)
	{
		SharedPreferences.Editor e = prefs(c).edit();
		if (iconIndex >= 0)
			e.putString("icon_" + id, ICON_KEYS[iconIndex]);
		else
			e.remove("icon_" + id);
		if (colorIndex >= 0)
			e.putInt("color_" + id, colorIndex);
		else
			e.remove("color_" + id);
		e.apply();
	}

	public static void clear(Context c, long id)
	{
		store(c, id, -1, -1);
	}

	// Draws a rounded colored tile with a white icon into the ImageView.
	public static void applyTile(ImageView view, int iconIndex, int colorIndex, float cornerDp)
	{
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(COLORS[colorIndex]);
		bg.setCornerRadius(dp(view.getContext(), cornerDp));
		view.setBackground(bg);
		view.setImageResource(ICON_RES[iconIndex]);
		view.setImageTintList(null);
		view.setColorFilter(Color.WHITE);
	}

	public static void applyTile(ImageView view, Context c, BookmarkBase b, float cornerDp)
	{
		applyTile(view, iconIndex(c, b), colorIndex(c, b), cornerDp);
	}

	private static float dp(Context c, float v)
	{
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
		                                 c.getResources().getDisplayMetrics());
	}

	// ---- picker dialog -------------------------------------------------------------

	public static void showPicker(Context c, BookmarkBase b, Runnable onChanged)
	{
		final long id = b.getId();
		final int[] icon = { storedIconIndex(c, id) };
		final int[] color = { storedColorIndex(c, id) };
		final int pad = (int)dp(c, 16);

		LinearLayout root = new LinearLayout(c);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setGravity(Gravity.CENTER_HORIZONTAL);
		root.setPadding(pad, pad, pad, 0);

		final ImageView preview = new ImageView(c);
		int pv = (int)dp(c, 56);
		LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(pv, pv);
		plp.gravity = Gravity.CENTER_HORIZONTAL;
		plp.bottomMargin = pad;
		preview.setLayoutParams(plp);
		int ip = (int)dp(c, 12);
		preview.setPadding(ip, ip, ip, ip);
		root.addView(preview);

		final GridLayout icons = new GridLayout(c);
		icons.setColumnCount(ICON_KEYS.length / 2);
		final GridLayout colors = new GridLayout(c);
		colors.setColumnCount(COLORS.length / 2);

		final Runnable refresh = new Runnable() {
			@Override public void run()
			{
				int ii = icon[0] >= 0 ? icon[0] : autoIconIndex(b);
				int ci = color[0] >= 0 ? color[0] : autoColorIndex(b);
				applyTile(preview, ii, ci, 18);
				for (int i = 0; i < icons.getChildCount(); i++)
					icons.getChildAt(i).setAlpha(i == ii ? 1.0f : 0.35f);
				for (int i = 0; i < colors.getChildCount(); i++)
				{
					View v = colors.getChildAt(i);
					v.setScaleX(i == ci ? 1.0f : 0.7f);
					v.setScaleY(i == ci ? 1.0f : 0.7f);
				}
			}
		};

		root.addView(label(c, R.string.bookmark_appearance_icon));
		int cell = (int)dp(c, 44);
		int m = (int)dp(c, 5);
		for (int i = 0; i < ICON_KEYS.length; i++)
		{
			final int idx = i;
			ImageView iv = new ImageView(c);
			GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
			lp.width = cell;
			lp.height = cell;
			lp.setMargins(m, m, m, m);
			iv.setLayoutParams(lp);
			int p = (int)dp(c, 10);
			iv.setPadding(p, p, p, p);
			applyTile(iv, i, 7, 12);
			iv.setOnClickListener(v -> {
				icon[0] = idx;
				refresh.run();
			});
			icons.addView(iv);
		}
		root.addView(icons);

		root.addView(label(c, R.string.bookmark_appearance_color));
		for (int i = 0; i < COLORS.length; i++)
		{
			final int idx = i;
			View dot = new View(c);
			GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
			lp.width = cell;
			lp.height = cell;
			lp.setMargins(m, m, m, m);
			dot.setLayoutParams(lp);
			GradientDrawable d = new GradientDrawable();
			d.setShape(GradientDrawable.OVAL);
			d.setColor(COLORS[i]);
			dot.setBackground(d);
			dot.setOnClickListener(v -> {
				color[0] = idx;
				refresh.run();
			});
			colors.addView(dot);
		}
		root.addView(colors);
		refresh.run();

		AlertDialog dialog = new AlertDialog.Builder(c)
		    .setTitle(b.getLabel())
		    .setView(root)
		    .setPositiveButton(R.string.bookmark_appearance_save,
		                       (d, w) -> {
			                       store(c, id, icon[0], color[0]);
			                       if (onChanged != null)
				                       onChanged.run();
		                       })
		    .setNeutralButton(R.string.bookmark_appearance_auto,
		                      (d, w) -> {
			                      clear(c, id);
			                      if (onChanged != null)
				                      onChanged.run();
		                      })
		    .setNegativeButton(android.R.string.cancel, null)
		    .show();
		// Tablets give dialogs a wide minimum width; keep this one as narrow as its content.
		if (dialog.getWindow() != null)
			dialog.getWindow().setLayout((int)dp(c, 340),
			                             android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	private static TextView label(Context c, int res)
	{
		TextView t = new TextView(c);
		t.setText(res);
		t.setTextAppearance(android.R.style.TextAppearance_Material_Subhead);
		t.setPadding(0, (int)dp(c, 8), 0, (int)dp(c, 2));
		t.setLayoutParams(new LinearLayout.LayoutParams(
		    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
		    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		return t;
	}
}
