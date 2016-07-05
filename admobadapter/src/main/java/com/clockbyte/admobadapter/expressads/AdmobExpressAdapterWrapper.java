/*
 *  Copyright 2015 Yahoo Inc. All rights reserved.
 * Copyright 2015 Clockbyte LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clockbyte.admobadapter.expressads;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.clockbyte.admobadapter.AdmobFetcherBase;
import com.clockbyte.admobadapter.R;
import com.google.android.gms.ads.NativeExpressAdView;

/**
 * Adapter that has common functionality for any adapters that need to show ads in-between
 * other data.
 */
public class AdmobExpressAdapterWrapper extends BaseAdapter implements AdmobFetcherBase.AdmobListener {

    private final String TAG = AdmobExpressAdapterWrapper.class.getCanonicalName();

    private BaseAdapter mAdapter;

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(BaseAdapter adapter) {
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                notifyDataSetInvalidated();
            }
        });
    }

    AdmobFetcherExpress adFetcher;
    Context mContext;

    private static final int VIEW_TYPE_COUNT = 1;
    private static final int VIEW_TYPE_AD_EXPRESS = 1;

    private final static int DEFAULT_NO_OF_DATA_BETWEEN_ADS = 10;
    private final static int DEFAULT_LIMIT_OF_ADS = 3;

    private int mNoOfDataBetweenAds;

    /*
    * Gets the number of your data items between ad blocks, by default it equals to 10.
    * You should set it according to the Admob's policies and rules which says not to
    * display more than one ad block at the visible part of the screen
    * so you should choose this parameter carefully and according to your item's height and screen resolution of a target devices
    */
    public int getNoOfDataBetweenAds() {
        return mNoOfDataBetweenAds;
    }

    /*
    * Sets the number of your data items between ad blocks, by default it equals to 10.
    * You should set it according to the Admob's policies and rules which says not to
    * display more than one ad block at the visible part of the screen
    * so you should choose this parameter carefully and according to your item's height and screen resolution of a target devices
    */
    public void setNoOfDataBetweenAds(int mNoOfDataBetweenAds) {
        this.mNoOfDataBetweenAds = mNoOfDataBetweenAds;
    }

    private int firstAdIndex = 0;

    public int getFirstAdIndex() {
        return firstAdIndex;
    }

    /*
    * Sets the first ad block index (zero-based) in the adapter, by default it equals to 0
    */
    public void setFirstAdIndex(int firstAdIndex) {
        this.firstAdIndex = firstAdIndex;
    }

    private int mLimitOfAds;

    /*
    * Gets the max count of ad blocks per dataset, by default it equals to 3 (according to the Admob's policies and rules)
    */
    public int getLimitOfAds() {
        return mLimitOfAds;
    }

    /*
    * Sets the max count of ad blocks per dataset, by default it equals to 3 (according to the Admob's policies and rules)
    */
    public void setLimitOfAds(int mLimitOfAds) {
        this.mLimitOfAds = mLimitOfAds;
    }

    private int mExpressAdsLayoutId;

    /*
    * Gets the res layout id for published express ads
    */
    public int getExpressAdsLayoutId() {
        return mExpressAdsLayoutId;
    }

    /*
    * Sets the res layout id for published express ads
    */
    public void setExpressAdsLayoutId(int mExpressAdsLayoutId) {
        this.mExpressAdsLayoutId = mExpressAdsLayoutId;
    }

    /*
    *Sets a test device ID. Normally you don't have to set it
    */
    public void setTestDeviceId(String testDeviceId) {
        adFetcher.setTestDeviceId(testDeviceId);
    }

    public AdmobExpressAdapterWrapper(Context context) {
        setNoOfDataBetweenAds(DEFAULT_NO_OF_DATA_BETWEEN_ADS);
        setLimitOfAds(DEFAULT_LIMIT_OF_ADS);
        setExpressAdsLayoutId(R.layout.adexpresslistview_item);
        mContext = context;

        adFetcher = new AdmobFetcherExpress(mContext);
        adFetcher.addListener(this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        switch (getItemViewType(position)) {
            case VIEW_TYPE_AD_EXPRESS:
                NativeExpressAdView item = null;
                if (convertView == null) {
                    item = getExpressAdView(parent);
                    adFetcher.setupAd(item);
                    adFetcher.fetchAd(item);
                } else {
                    item = (NativeExpressAdView) convertView;
                }
                return item;
            default:
                int origPos = getOriginalContentPosition(position);
                return mAdapter.getView(origPos, convertView, parent);
        }
    }

    private NativeExpressAdView getExpressAdView(ViewGroup parent) {
        // Inflate a layout and add it to the parent ViewGroup.
        LayoutInflater inflater = (LayoutInflater) parent.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NativeExpressAdView adView = (NativeExpressAdView) inflater
                .inflate(getExpressAdsLayoutId(), parent, false);
        return adView;
    }

    /**
     * <p>Gets the count of all data, including interspersed ads.</p>
     * <p/>
     * <p>If data size is 10 and an ad is to be showed after every 5 items starting at the index 0, this method
     * will return 12.</p>
     *
     * @return the total number of items this adapter can show, including ads.
     * @see AdmobExpressAdapterWrapper#setNoOfDataBetweenAds(int)
     * @see AdmobExpressAdapterWrapper#getNoOfDataBetweenAds()
     */
    @Override
    public int getCount() {

        if (mAdapter != null) {
            /*
            No of currently fetched ads, as long as it isn't more than no of max ads that can
            fit dataset.
             */
            int noOfAds = getAdsCountToPublish();
            return mAdapter.getCount() > 0 ? mAdapter.getCount() + noOfAds : 0;
        } else {
            return 0;
        }
    }

    public int getAdsCountToPublish(){
        //int cntFetched = adFetcher.getFetchedAdsCount();
        //if(cntFetched == 0) return 0;
        int expected = 0;
        if(mAdapter.getCount() > 0 && mAdapter.getCount()>= getOffsetValue()+1)
            expected = (mAdapter.getCount() - getOffsetValue()) / getNoOfDataBetweenAds() + 1;
        expected = Math.max(0, expected);
        //int noOfAds = Math.min(cntFetched, expected);
        return Math.min(expected, getLimitOfAds());
    }

    /**
     * Gets the item in a given position in the dataset. If an ad is to be returned,
     * a {@link NativeExpressAdView} object is returned.
     *
     * @param position the adapter position
     * @return the object or ad contained in this adapter position
     */
    @Override
    public Object getItem(int position) {

        if (canShowAdAtPosition(position)) {
            int adPos = getAdIndex(position);
            return adFetcher.getAdForIndex(adPos);
        } else {
            int origPos = getOriginalContentPosition(position);
            return mAdapter.getItem(origPos);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT + getAdapter().getViewTypeCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (canShowAdAtPosition(position)) {
            return VIEW_TYPE_AD_EXPRESS;
        } else {
            int origPos = getOriginalContentPosition(position);
            return mAdapter.getItemViewType(origPos);
        }
    }

    /**
     * Translates an adapter position to an actual position within the underlying dataset.
     *
     * @param position the adapter position
     * @return the original position that the adapter position would have been without ads
     */
    protected int getOriginalContentPosition(int position) {
        int noOfAds = getAdsCountToPublish();
        // No of spaces for ads in the dataset, according to ad placement rules
        int adSpacesCount = (getAdIndex(position) + 1);
        int originalPosition = position - Math.min(adSpacesCount, noOfAds);
        Log.d("POSITION", position + " is originally " + originalPosition);

        return originalPosition;
    }

    /**
     * Determines if an ad can be shown at the given position. Checks if the position is for
     * an ad, using the preconfigured ad positioning rules; and if a native ad object is
     * available to place in that position.
     *
     * @param position the adapter position
     * @return <code>true</code> if ads can
     */
    protected boolean canShowAdAtPosition(int position) {

        // Is this a valid position for an ad?
        // Is an ad for this position available?
        return isAdPosition(position) && isAdAvailable(position);
    }

    /**
     * Gets the ad index for this adapter position within the list of currently fetched ads.
     *
     * @param position the adapter position
     * @return the index of the ad within the list of fetched ads
     */
    private int getAdIndex(int position) {
        int index = -1;
        if(position >= getOffsetValue())
            index = (position - getOffsetValue()) / (getNoOfDataBetweenAds()+1);
        Log.d("POSITION", "index " + index + " for position " + position);
        return index;
    }

    /**
     * Checks if adapter position is an ad position.
     *
     * @param position the adapter position
     * @return {@code true} if an ad position, {@code false} otherwise
     */
    private boolean isAdPosition(int position) {
        int result = (position - getOffsetValue()) % (getNoOfDataBetweenAds() + 1);
        return result == 0;
    }

    private int getOffsetValue() {
        return getFirstAdIndex() > 0 ? getFirstAdIndex() : 0;
    }

    /**
     * Checks if an ad is available for this position.
     *
     * @param position the adapter position
     * @return {@code true} if an ad is available, {@code false} otherwise
     */
    private boolean isAdAvailable(int position) {
        int adIndex = getAdIndex(position);
        int firstAdPos = getOffsetValue();

        return position >= firstAdPos && adIndex >= 0 && adIndex < getLimitOfAds();
    }

    /**
     * Destroys all currently fetched ads
     */
    public void destroyAds() {
        adFetcher.destroyAllAds();
    }

    /**
     * Clears all currently displaying ads to update them
     */
    public void requestUpdateAd() {
        adFetcher.updateAds();
    }

    @Override
    public void onAdCountChanged() {

        notifyDataSetChanged();
    }
}
