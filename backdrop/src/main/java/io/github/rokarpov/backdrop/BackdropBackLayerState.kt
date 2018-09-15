package io.github.rokarpov.backdrop

import android.view.View

internal enum class BackdropBackLayerState {
    CONCEALED {
        override fun getContentHeight(interactionData: BackdropBackLayerInteractionData?, contentView: View?, headerView: View): Int {
            return 0
        }
        override fun onLayout(layout: BackdropBackLayer) { }
    },
    REVEALED {
        override fun getContentHeight(interactionData: BackdropBackLayerInteractionData?, contentView: View?, headerView: View): Int {
            return if ((contentView != null) && (interactionData != null)) {
                headerView.measuredHeight - interactionData.getLayoutRevealedHeight(contentView, headerView)
            } else {
                0
            }
        }
        override fun onLayout(layout: BackdropBackLayer) {
            val view = layout.revealedView
            if (view != null) {
                BackdropBackLayerInteractionData.showView(view)
            }
        }
    };

    internal abstract fun getContentHeight(interactionData: BackdropBackLayerInteractionData?, contentView: View?, headerView: View): Int
    internal abstract fun onLayout(layout: BackdropBackLayer)
}