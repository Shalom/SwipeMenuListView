package com.baoyz.swipemenulistview;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * @author baoyz
 * @date 2014-8-18
 */
public class SwipeMenuListView extends ListView {

	private static final int TOUCH_STATE_NONE = 0;
	private static final int TOUCH_STATE_X = 1;
	private static final int TOUCH_STATE_Y = 2;

	private int MAX_Y = 5;
	private int MAX_X = 3;
	private float mDownX;
	private float mDownY;
	private int mTouchState;
	private int mTouchPosition;
	private SwipeMenuLayout mTouchView;
	private OnSwipeListener mOnSwipeListener;

	private SwipeMenuCreator mMenuCreator;
	private OnMenuItemClickListener mOnMenuItemClickListener;
	private Interpolator mCloseInterpolator;
	private Interpolator mOpenInterpolator;
	private SwipeMenuFilter mMenuFilter;

	public SwipeMenuListView(Context context) {
		super(context);
		init();
	}

	public SwipeMenuListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public SwipeMenuListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		MAX_X = dp2px(MAX_X);
		MAX_Y = dp2px(MAX_Y);
		mTouchState = TOUCH_STATE_NONE;
	}

	public boolean isMenuOpen() {
		return mTouchView != null && mTouchView.isOpen();
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(new SwipeMenuAdapter(getContext(), adapter) {
			@Override
			public void createMenu(SwipeMenu menu) {
				if (mMenuCreator != null) {
					mMenuCreator.create(menu);
				}
			}

			@Override
			public void onItemClick(SwipeMenuView view, SwipeMenu menu, int index) {
				boolean flag = false;
				if (mOnMenuItemClickListener != null) {
					flag = mOnMenuItemClickListener.onMenuItemClick(view.getPosition(), menu, index);
				}
				if (mTouchView != null && !flag) {
					mTouchView.smoothCloseMenu();
				}
			}
		});
	}

	public void setCloseInterpolator(Interpolator interpolator) {
		mCloseInterpolator = interpolator;
	}

	public void setOpenInterpolator(Interpolator interpolator) {
		mOpenInterpolator = interpolator;
	}

	public Interpolator getOpenInterpolator() {
		return mOpenInterpolator;
	}

	public Interpolator getCloseInterpolator() {
		return mCloseInterpolator;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev.getAction() != MotionEvent.ACTION_DOWN && mTouchView == null)
			return super.onTouchEvent(ev);
		int action = MotionEventCompat.getActionMasked(ev);
		action = ev.getAction();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				int oldPos = mTouchPosition;
				mDownX = ev.getX();
				mDownY = ev.getY();
				mTouchState = TOUCH_STATE_NONE;

				mTouchPosition = pointToPosition((int) ev.getX(), (int) ev.getY());

				if (mTouchPosition == oldPos && mTouchView != null && mTouchView.isOpen()) {
					mTouchState = TOUCH_STATE_X;
					mTouchView.onSwipe(ev);
					return true;
				}

				View view = getChildAt(mTouchPosition - getFirstVisiblePosition());

				if (mTouchView != null && mTouchView.isOpen()) {
					mTouchView.smoothCloseMenu();
					mTouchView = null;
					// try to cancel the touch event
					MotionEvent cancelEvent = MotionEvent.obtain(ev);
					cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
					onTouchEvent(cancelEvent);
					cancelEvent.recycle();
					return true;
				}
				if (view instanceof SwipeMenuLayout) {
					mTouchView = (SwipeMenuLayout) view;
				}
				if (mTouchView != null) {
					mTouchView.onSwipe(ev);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				float dy = Math.abs((ev.getY() - mDownY));
				float dx = Math.abs((ev.getX() - mDownX));
				if (mTouchState == TOUCH_STATE_X) {
					if (mTouchView != null) {
						mTouchView.onSwipe(ev);
					}
					getSelector().setState(new int[] { 0 });
					ev.setAction(MotionEvent.ACTION_CANCEL);
					super.onTouchEvent(ev);
					return true;
				} else if (mTouchState == TOUCH_STATE_NONE) {
					if (Math.abs(dy) > MAX_Y) {
						mTouchState = TOUCH_STATE_Y;
					} else if (dx > MAX_X) {
						mTouchState = TOUCH_STATE_X;
						if (mOnSwipeListener != null) {
							mOnSwipeListener.onSwipeStart(mTouchPosition);
						}
						// Apply the menu filter on the Swipe menu items in case one was set
						filterMenu(mTouchPosition);
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mTouchState == TOUCH_STATE_X) {
					if (mTouchView != null) {
						mTouchView.onSwipe(ev);
						if (!mTouchView.isOpen()) {
							mTouchPosition = -1;
							mTouchView = null;
						}
					}
					if (mOnSwipeListener != null) {
						mOnSwipeListener.onSwipeEnd(mTouchPosition);
					}
					ev.setAction(MotionEvent.ACTION_CANCEL);
					super.onTouchEvent(ev);
					return true;
				}
				break;
		}
		return super.onTouchEvent(ev);
	}

	public void smoothOpenMenu(int position) {
		if (position >= getFirstVisiblePosition() && position <= getLastVisiblePosition()) {
			View view = getChildAt(position - getFirstVisiblePosition());
			if (view instanceof SwipeMenuLayout) {
				mTouchPosition = position;
				if (mTouchView != null && mTouchView.isOpen()) {
					mTouchView.smoothCloseMenu();
				}
				mTouchView = (SwipeMenuLayout) view;
				mTouchView.smoothOpenMenu();
			}
		}
	}

	private int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext()
				.getResources().getDisplayMetrics());
	}

	public void setMenuCreator(SwipeMenuCreator menuCreator) {
		this.mMenuCreator = menuCreator;
	}

	public void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
		this.mOnMenuItemClickListener = onMenuItemClickListener;
	}

	/**
	 * Set a menu filter that will be dynamically called every time the swipe
	 * begins to determine if some of the items should be hidden.
	 * 
	 * @param menuFilter
	 */
	public void setMenuFilter(SwipeMenuFilter menuFilter) {
		this.mMenuFilter = menuFilter;
	}

	public void setOnSwipeListener(OnSwipeListener onSwipeListener) {
		this.mOnSwipeListener = onSwipeListener;
	}

	public static interface OnMenuItemClickListener {
		boolean onMenuItemClick(int position, SwipeMenu menu, int index);
	}

	public static interface OnSwipeListener {
		void onSwipeStart(int position);

		void onSwipeEnd(int position);
	}

	/**
	 * Apply the {@link SwipeMenuFilter} on the items.
	 * 
	 * @param rowPosition The list row position.
	 */
	private void filterMenu(int rowPosition) {
		if (mMenuFilter != null && mTouchView != null) {
			// Delegate this work to the menu layout. The menu views are nested deep
			// in there.
			SwipeMenuView menuView = mTouchView.getMenuView();
			// Traverse the children of the menu view. At this point, we use every
			// child ID in order to apply the filter.
			int childCount = menuView.getChildCount();
			for (int i = 0; i < childCount; i++) {
				View menuItem = menuView.getChildAt(i);
				boolean isVisible = mMenuFilter.isVisible(rowPosition, menuItem.getId());
				menuItem.setVisibility(isVisible ? View.VISIBLE : View.GONE);
			}
		}
	}

	/**
	 * A swipe menu filter allows dynamic filtering of menu items when the swipe
	 * begins. In case one is registered, every menu item in the
	 * about-to-be-opened menu will be queried against the listener to determine
	 * its visibility.
	 * 
	 * @author Shalom Gibly
	 */
	public static interface SwipeMenuFilter {

		/**
		 * Returns if the menu item with the given ID at the given row should be
		 * visible.
		 * 
		 * @param rowPosition
		 * @param menuItemId A unique ID for the menu item.
		 * @return <code>true</code> for visible menu items, <code>false</code> to
		 *         hide them. Use SwipeMenuItem#setId(int) for a way to make sure
		 *         those IDs are unique.
		 * @see SwipeMenuItem#setId(int)
		 */
		boolean isVisible(int rowPosition, int menuItemId);
	}
}
