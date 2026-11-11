/*
   RecyclerView adapter for bookmark list items

   Copyright 2026 Ibrahim Sevinc <ibrahim.sevinc.mail@gmail.com>

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
   If a copy of the MPL was not distributed with this file, You can obtain one at
   http://mozilla.org/MPL/2.0/.
*/

package com.freerdp.freerdpcore.utils;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.freerdp.freerdpcore.R;
import com.freerdp.freerdpcore.domain.BookmarkBase;
import com.freerdp.freerdpcore.domain.ConnectionReference;
import com.freerdp.freerdpcore.presentation.BookmarkActivity;

import java.util.ArrayList;
import java.util.List;

public class BookmarkListAdapter extends RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>
{
	public interface Callbacks
	{
		void onItemClick(String refStr);
		void onDelete(long id);
		void onExport(BookmarkBase bookmark);
	}

	private List<BookmarkBase> items = new ArrayList<>();
	private Callbacks callbacks;
	private boolean actionsEnabled = true;
	private boolean gridMode = false;

	private static final int TYPE_LIST = 0;
	private static final int TYPE_GRID = 1;

	// Grid (tile) or list layout for the items.
	public void setGridMode(boolean grid)
	{
		if (gridMode == grid)
			return;
		gridMode = grid;
		notifyDataSetChanged();
	}

	@Override public int getItemViewType(int position)
	{
		return gridMode ? TYPE_GRID : TYPE_LIST;
	}

	public void setCallbacks(Callbacks callbacks)
	{
		this.callbacks = callbacks;
	}

	public void setActionsEnabled(boolean enabled)
	{
		actionsEnabled = enabled;
	}

	public List<BookmarkBase> getItems()
	{
		return items;
	}

	public void setItems(List<BookmarkBase> newItems)
	{
		items = newItems != null ? newItems : new ArrayList<>();
		notifyDataSetChanged();
	}

	@NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		final int layout =
		    viewType == TYPE_GRID ? R.layout.bookmark_grid_item : R.layout.bookmark_list_item;
		return new ViewHolder(
		    LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
	}

	@Override public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		BookmarkBase bookmark = items.get(position);
		String refStr;

		holder.bookmarkText1.setText(bookmark.getLabel());

		if (bookmark.getType() == BookmarkBase.TYPE_MANUAL)
		{
			BookmarkAppearance.applyTile(holder.bookmarkIcon1, holder.itemView.getContext(),
			                             bookmark, gridMode ? 22 : 10);
			holder.bookmarkIcon1.setVisibility(View.VISIBLE);
			holder.bookmarkText2.setVisibility(View.VISIBLE);
			holder.bookmarkText2.setText(bookmark.getHostname());
			refStr = ConnectionReference.getBookmarkReference(bookmark.getId());

			if (actionsEnabled)
			{
				holder.bookmarkIcon2.setVisibility(View.VISIBLE);
				holder.bookmarkIcon2.setImageResource(R.drawable.ic_more_vert);
				final String finalRefStr = refStr;
				final long bookmarkId = bookmark.getId();
				holder.bookmarkIcon2.setOnClickListener(
				    v -> showBookmarkMenu(v, finalRefStr, bookmarkId, bookmark));
				holder.itemView.setOnLongClickListener(v -> {
					showBookmarkMenu(holder.bookmarkIcon2, finalRefStr, bookmarkId,
					                 bookmark);
					return true;
				});
			}
			else
			{
				holder.bookmarkIcon2.setVisibility(View.GONE);
				holder.bookmarkIcon2.setOnClickListener(null);
				holder.itemView.setOnLongClickListener(null);
			}
		}
		else if (bookmark.getType() == BookmarkBase.TYPE_QUICKCONNECT)
		{
			holder.bookmarkIcon1.setBackground(null);
			holder.bookmarkIcon1.clearColorFilter();
			holder.bookmarkText2.setVisibility(View.GONE);
			refStr = ConnectionReference.getHostnameReference(bookmark.getHostname());
			holder.itemView.setOnLongClickListener(null);

			if (bookmark.isDirectConnect())
			{
				holder.bookmarkText1.setText(holder.itemView.getContext().getString(
				    R.string.quick_connect_to, bookmark.getHostname()));
				holder.bookmarkIcon1.setImageResource(R.drawable.ic_login);
				holder.bookmarkIcon1.setVisibility(View.VISIBLE);
				holder.bookmarkIcon2.setVisibility(View.GONE);
				holder.bookmarkIcon2.setOnClickListener(null);
			}
			else
			{
				holder.bookmarkText1.setText(bookmark.getLabel());
				holder.bookmarkIcon1.setImageResource(R.drawable.ic_history);
				holder.bookmarkIcon1.setVisibility(View.VISIBLE);
				holder.bookmarkIcon2.setVisibility(View.VISIBLE);
				holder.bookmarkIcon2.setImageResource(R.drawable.ic_bookmark_add);
				final String hostname = bookmark.getHostname();
				holder.bookmarkIcon2.setOnClickListener(v -> {
					Bundle bundle = new Bundle();
					bundle.putString(BookmarkActivity.PARAM_CONNECTION_REFERENCE,
					                 ConnectionReference.getHostnameReference(hostname));
					Intent intent = new Intent(v.getContext(), BookmarkActivity.class);
					intent.putExtras(bundle);
					v.getContext().startActivity(intent);
				});
			}
		}
		else
		{
			holder.bookmarkIcon1.setVisibility(View.GONE);
			holder.bookmarkText2.setVisibility(View.GONE);
			refStr = "";
			holder.bookmarkIcon2.setVisibility(View.GONE);
			holder.bookmarkIcon2.setOnClickListener(null);
		}

		holder.itemView.setTag(refStr);

		holder.itemView.setOnClickListener(v -> {
			if (callbacks != null)
				callbacks.onItemClick(v.getTag().toString());
		});
	}

	@Override public int getItemCount()
	{
		return items.size();
	}

	private void showBookmarkMenu(View anchor, String refStr, long bookmarkId,
	                              BookmarkBase bookmark)
	{
		PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
		popup.inflate(R.menu.bookmark_context_menu);
		popup.setOnMenuItemClickListener(item -> {
			int itemId = item.getItemId();
			if (itemId == R.id.bookmark_connect)
			{
				if (callbacks != null)
					callbacks.onItemClick(refStr);
				return true;
			}
			else if (itemId == R.id.bookmark_edit)
			{
				Bundle bundle = new Bundle();
				bundle.putString(BookmarkActivity.PARAM_CONNECTION_REFERENCE, refStr);
				Intent intent = new Intent(anchor.getContext(), BookmarkActivity.class);
				intent.putExtras(bundle);
				anchor.getContext().startActivity(intent);
				return true;
			}
			else if (itemId == R.id.bookmark_delete)
			{
				if (callbacks != null)
					callbacks.onDelete(bookmarkId);
				return true;
			}
			else if (itemId == R.id.bookmark_appearance)
			{
				BookmarkAppearance.showPicker(anchor.getContext(), bookmark,
				                              this::notifyDataSetChanged);
				return true;
			}
			else if (itemId == R.id.bookmark_export)
			{
				if (callbacks != null)
					callbacks.onExport(bookmark);
				return true;
			}
			return false;
		});
		popup.show();
	}

	static class ViewHolder extends RecyclerView.ViewHolder
	{
		final ImageView bookmarkIcon1;
		final ImageView bookmarkIcon2;
		final TextView bookmarkText1;
		final TextView bookmarkText2;

		ViewHolder(View view)
		{
			super(view);
			bookmarkIcon1 = view.findViewById(R.id.bookmark_icon1);
			bookmarkIcon2 = view.findViewById(R.id.bookmark_icon2);
			bookmarkText1 = view.findViewById(R.id.bookmark_text1);
			bookmarkText2 = view.findViewById(R.id.bookmark_text2);
		}
	}
}
