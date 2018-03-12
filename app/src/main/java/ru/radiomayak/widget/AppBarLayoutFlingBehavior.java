package ru.radiomayak.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public final class AppBarLayoutFlingBehavior extends AppBarLayout.Behavior {
    private boolean isPositive;

    public AppBarLayoutFlingBehavior() {
    }

    public AppBarLayoutFlingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, AppBarLayout appBarLayout) {
        Parcelable parcelable = super.onSaveInstanceState(parent, appBarLayout);
        return new InstanceState(parcelable);
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, AppBarLayout appBarLayout, Parcelable parcelable) {
        if (parcelable instanceof InstanceState) {
            InstanceState state = (InstanceState) parcelable;
            super.onRestoreInstanceState(parent, appBarLayout, state.getSuperState());
        } else {
            super.onRestoreInstanceState(parent, appBarLayout, parcelable);
        }
    }

    @Override
    public boolean onNestedFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull AppBarLayout child,
            @NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (velocityY > 0 && !isPositive || velocityY < 0 && isPositive) {
            velocityY = velocityY * -1;
        }
        if (target instanceof SwipeRefreshLayout && velocityY < 0) {
            target = ((SwipeRefreshLayout) target).getChildAt(0);
        }
        if (target instanceof RecyclerView && velocityY < 0) {
            final RecyclerView recyclerView = (RecyclerView) target;
            final View firstChild = recyclerView.getChildAt(0);
            final int childAdapterPosition = recyclerView.getChildAdapterPosition(firstChild);
            consumed = childAdapterPosition > 0 || firstChild.getTop() < 0;
        }
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child,
            View target, int dx, int dy, int[] consumed, int type) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        isPositive = dy > 0;
    }

    protected static class InstanceState extends AppBarLayout.Behavior.SavedState {
        public InstanceState(Parcel source, ClassLoader loader) {
            super(source, loader);
        }

        public InstanceState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
        }

        public static final Parcelable.Creator<InstanceState> CREATOR =
                new Parcelable.ClassLoaderCreator<InstanceState>() {
                    @Override
                    public InstanceState createFromParcel(Parcel source) {
                        return new InstanceState(source, getClass().getClassLoader());
                    }

                    @Override
                    public InstanceState[] newArray(int size) {
                        return new InstanceState[size];
                    }

                    @Override
                    public InstanceState createFromParcel(Parcel source, ClassLoader loader) {
                        return new InstanceState(source, loader);
                    }
                };
//                ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<InstanceState>() {
//                    @Override
//                    public InstanceState createFromParcel(Parcel source, ClassLoader loader) {
//                        return new InstanceState(source, loader);
//                    }
//
//                    @Override
//                    public InstanceState[] newArray(int size) {
//                        return new InstanceState[size];
//                    }
//                });
    }
}