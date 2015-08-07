package com.github.florent37.beautifulviewpager;

import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.florent37.beautifulviewpager.recycler.HeaderHolder;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by florentchampigny on 07/08/15.
 */
public class BeautifulViewPagerAnimator implements ViewPager.OnPageChangeListener {
    BeautifulViewPager bvp;

    float initialHeaderHeight = -1;
    float finalHeaderHeight = -1;
    List<Object> scrolls = new ArrayList<>();
    List<Object> calledScrolls = new ArrayList<>();

    int oldpage = -2;

    protected void onPageScroll(int position) {
        if (position != oldpage) {
            oldpage = position;
            for (int i = 0, size = bvp.headerHolders.size(); i < size; ++i) {
                bvp.headerHolders.get(i).view.animate().cancel();
                if (i == position) {
                    bvp.headerHolders.get(i).view.animate().setDuration(300).alpha(1).start();

                    Rect rect = new Rect();
                    if (!bvp.headerHolders.get(i).view.getGlobalVisibleRect(rect)) {
                        if (i - 1 > 0)
                            bvp.headerScroll.smoothScrollTo(bvp.headerHolders.get(i - 1).view.getLeft(), 0);
                        else
                            bvp.headerScroll.smoothScrollTo(bvp.headerHolders.get(i).view.getLeft(), 0);
                    }

                } else {
                    if (bvp.headerHolders.get(i).view.getAlpha() != 0.5)
                        bvp.headerHolders.get(i).view.animate().setDuration(300).alpha(0.5f);
                }
            }
        }
    }

    public BeautifulViewPagerAnimator(BeautifulViewPager beautifulViewPager) {
        this.bvp = beautifulViewPager;

        bvp.viewPager.setPageMargin(bvp.getContext().getResources().getDimensionPixelOffset(R.dimen.viewpager_margin));
        bvp.viewPager.addOnPageChangeListener(this);
    }

    public void onScroll(Object source, int verticalOffset) {
        dispatchScroll(source, verticalOffset);

        bvp.headerScroll.setTranslationY(-verticalOffset);
        if (bvp.headerScroll.getTranslationY() > 0)
            bvp.headerScroll.setTranslationY(0);

        if (initialHeaderHeight <= 0) {
            initialHeaderHeight = bvp.headerScroll.getHeight();
            finalHeaderHeight = initialHeaderHeight / 2;
        }
        if (initialHeaderHeight > 0) {

            if (verticalOffset < finalHeaderHeight) {
                float percent = (initialHeaderHeight - verticalOffset) / initialHeaderHeight;

                bvp.headerLayout.setPivotX(0);
                //headerLayout.setPivotY(0);
                bvp.headerLayout.setScaleX(percent);
                bvp.headerLayout.setScaleY(percent);
                bvp.headerLayout.setTranslationY(verticalOffset / 2);

                bvp.headerLayout.setTranslationX(1.5f * verticalOffset);

                float alphaPercent = 1 - verticalOffset / finalHeaderHeight;
                for (int i = 0, count = bvp.headerHolders.size(); i < count; ++i) {
                    HeaderHolder headerHolder = bvp.headerHolders.get(i);
                    headerHolder.textView.setAlpha(alphaPercent);

                    headerHolder.view.setTranslationX(-i * 4 * headerHolder.view.getPaddingLeft() * (1 - percent));
                }
            }
        }
    }

    Random rand = new Random();

    public float random() {
        float minX = 0.4f;
        float maxX = 0.8f;
        return rand.nextFloat() * (maxX - minX) + minX;
    }

    protected void fillHeader(PagerAdapter adapter) {
        bvp.headerLayout.removeAllViews();

        LayoutInflater layoutInflater = LayoutInflater.from(bvp.getContext());
        for (int i = 0; i < adapter.getCount(); ++i) {
            View view = layoutInflater.inflate(R.layout.header_card, bvp.headerLayout, false);
            bvp.headerLayout.addView(view);

            HeaderHolder headerHolder = new HeaderHolder(view, (TextView) view.findViewById(R.id.title), view.findViewById(R.id.card));
            bvp.headerHolders.add(headerHolder);

            headerHolder.textView.setText(adapter.getPageTitle(i));

            ViewGroup.LayoutParams layoutParams = headerHolder.card.getLayoutParams();
            layoutParams.height = (int) (view.getLayoutParams().height * random());
            headerHolder.card.setLayoutParams(layoutParams);

            if (i == 0) {
                bvp.headerHolders.get(i).view.setAlpha(1f);
            } else {
                bvp.headerHolders.get(i).view.setAlpha(0.5f);
            }

            final int position = i;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onHeaderClick(position);
                }
            });
        }
    }


    public void onHeaderClick(int position) {
        bvp.viewPager.setCurrentItem(position, true);
    }

    public void registerRecyclerView(RecyclerView recyclerView) {
        scrolls.add(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (calledScrolls.contains(recyclerView))
                    calledScrolls.remove(recyclerView);
                else {
                    onScroll(recyclerView, recyclerView.computeVerticalScrollOffset());
                }
            }
        });

    }

    public void registerScrollView(final ObservableScrollView scrollView) {
        scrolls.add(scrollView);

        if (scrollView.getParent() != null && scrollView.getParent().getParent() != null && scrollView.getParent().getParent() instanceof ViewGroup)
            scrollView.setTouchInterceptionViewGroup((ViewGroup) scrollView.getParent().getParent());

        scrollView.setScrollViewCallbacks(new ObservableScrollViewCallbacks() {
            @Override
            public void onScrollChanged(int i, boolean b, boolean b1) {
                if (calledScrolls.contains(scrollView))
                    calledScrolls.remove(scrollView);
                else {
                    onScroll(scrollView, i);
                }
            }

            @Override
            public void onDownMotionEvent() {

            }

            @Override
            public void onUpOrCancelMotionEvent(ScrollState scrollState) {

            }
        });
    }

    public void dispatchScroll(Object source, int yOffset) {
        for (Object scroll : scrolls) {
            if (scroll != source) {
                calledScrolls.add(scroll);
                if (scroll instanceof RecyclerView) {
                    RecyclerView recyclerView = (RecyclerView) scroll;
                    if (recyclerView != null && recyclerView.getLayoutManager() != null && recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        linearLayoutManager.scrollToPositionWithOffset(0, -yOffset);
                    }
                } else if (scroll instanceof ScrollView) {
                    ((ScrollView) scroll).scrollTo(0, yOffset);
                }
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        onPageScroll(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}