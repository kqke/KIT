package com.example.kit.util;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.kit.R;
import com.example.kit.models.ClusterMarker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

public class MyClusterManagerRenderer extends DefaultClusterRenderer<ClusterMarker>
{
  private final IconGenerator iconGenerator;
  private final ImageView imageView;
  private final int markerWidth;
  private final int markerHeight;
  private Context mContext;
  Bitmap icon;

  public MyClusterManagerRenderer(Context context, GoogleMap googleMap,
                                  ClusterManager<ClusterMarker> clusterManager) {
    super(context, googleMap, clusterManager);
    mContext = context;
// initialize cluster item icon generator
    iconGenerator = new IconGenerator(context.getApplicationContext());
    imageView = new ImageView(context.getApplicationContext());
    markerWidth = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
    markerHeight = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
    imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
    int padding = (int) context.getResources().getDimension(R.dimen.custom_marker_padding);
    imageView.setPadding(padding, padding, padding, padding);
    iconGenerator.setContentView(imageView);
  }

  /**
   * Rendering of the individual ClusterItems
   * @param item
   * @param markerOptions
   */
  @Override
  protected void onBeforeClusterItemRendered(ClusterMarker item, MarkerOptions markerOptions) {
    imageView.setImageResource(R.drawable.cartman_cop);
//    Glide.with(mContext).load(item.getIconPicture()).into(imageView);
    icon = iconGenerator.makeIcon();
    iconGenerator.setContentView(imageView);
    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(item.getTitle());
  }

  @Override
  protected void onClusterItemRendered(ClusterMarker clusterItem, final Marker marker) {
    super.onClusterItemRendered(clusterItem, marker);
    Glide.with(mContext)
            .load(clusterItem.getIconPicture())
            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
            .thumbnail(0.1f)
            .into(new SimpleTarget<Drawable>() {
              @Override
              public void onResourceReady(Drawable drawable, Transition<? super Drawable> transition) {
                imageView.setImageDrawable(drawable);
                icon = iconGenerator.makeIcon();
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
              }
            });
  }

  @Override
  protected boolean shouldRenderAsCluster(Cluster cluster) {
    return false;
  }

  /**
   * Update the GPS coordinate of a ClusterItem
   * @param clusterMarker
   */
  public void setUpdateMarker(ClusterMarker clusterMarker) {
    Marker marker = getMarker(clusterMarker);
    if (marker != null) {
      marker.setPosition(clusterMarker.getPosition());
    }
  }


}