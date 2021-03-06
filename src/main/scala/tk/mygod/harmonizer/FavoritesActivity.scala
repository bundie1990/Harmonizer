package tk.mygod.harmonizer

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view._
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import tk.mygod.app.{CircularRevealActivity, ToolbarActivity}
import tk.mygod.view.LocationObserver

import scala.collection.mutable.ArrayBuffer

/**
 * @author Mygod
 */
final class FavoritesActivity extends ToolbarActivity with CircularRevealActivity with TypedFindView {
  private final class FavoriteItemViewHolder(private val view: View) extends RecyclerView.ViewHolder(view)
    with View.OnClickListener {
    var item: FavoriteItem = _
    private val text = itemView.findViewById(android.R.id.text1).asInstanceOf[TextView]
    itemView.setOnTouchListener(LocationObserver)
    itemView.setOnClickListener(this)

    {
      val share = itemView.findViewById(R.id.action_share)
      share.setOnClickListener(_ =>
        FavoritesActivity.this.share(getString(R.string.share_content, item.getFullName), item.name))
      share.setOnLongClickListener(_ => {
        makeToast(R.string.action_share).show
        true
      })
    }

    def bind(item: FavoriteItem) {
      this.item = item
      text.setText(item.getFullName)
      view.setTag(item)
    }

    def onClick(v: View) {
      MainActivity.instance.frequencyText.setText(Utils.betterToString(item.frequency))
      supportFinishAfterTransition()
    }
  }

  private final class FavoritesAdapter(private val empty: View) extends RecyclerView.Adapter[FavoriteItemViewHolder] {
    private val favorites = new ArrayBuffer[FavoriteItem]
    private val pref = getSharedPreferences("favorites", Context.MODE_PRIVATE)

    {
      val size = pref.getInt("size", 0)
      if (size > 0) {
        empty.setVisibility(View.GONE)
        for (i <- 0 until size) favorites += new FavoriteItem(pref.getString(i + "_name", ""),
          java.lang.Double.longBitsToDouble(pref.getLong(i + "_freq", 0)))
      }
    }

    def onCreateViewHolder(vg: ViewGroup, i: Int) = new FavoriteItemViewHolder(LayoutInflater.from(vg.getContext)
      .inflate(R.layout.favorite_list_item, vg, false))
    def onBindViewHolder(vh: FavoriteItemViewHolder, i: Int) = vh.bind(favorites(i))
    def getItemCount = favorites.size

    def add(item: FavoriteItem) {
      undoManager.flush
      val pos = favorites.size
      favorites += item
      update
      empty.setVisibility(View.GONE)
      notifyItemInserted(pos)
    }

    def remove(pos: Int) {
      favorites.remove(pos)
      update
      notifyItemRemoved(pos)
      if (favorites.isEmpty) empty.setVisibility(View.VISIBLE)
    }
    def remove(item: FavoriteItem): Unit = remove(favorites.indexOf(item))

    def move(from: Int, to: Int) {
      if (from == to) return
      undoManager.flush
      val item = favorites(from)
      val order = if (from > to) -1 else 1
      var i = from
      var j = from + order
      while ((j <= from || j <= to) && (j >= from || j >= to)) {
        favorites(i) = favorites(j)
        i = j
        j += order
      }
      favorites(to) = item
      update
      notifyItemMoved(from, to)
    }

    def undo(actions: Iterator[(Int, FavoriteItem)]) = {
      for ((index, item) <- actions) {
        favorites.insert(index, item)
        notifyItemInserted(index)
      }
      update
      empty.setVisibility(View.GONE)
    }

    def update {
      val oldSize = pref.getInt("size", 0)
      var size = 0
      val editor = pref.edit
      for (favorite <- favorites) {
        editor.putString(size + "_name", favorite.name)
        editor.putLong(size + "_freq", java.lang.Double.doubleToLongBits(favorite.frequency))
        size += 1
      }
      editor.putInt("size", size)
      for (size <- size until oldSize) {
        editor.remove(size + "_name")
        editor.remove(size + "_freq")
      }
      editor.apply
    }
  }

  private var favoritesAdapter: FavoritesAdapter = _
  private var undoManager: UndoSnackbarManager[FavoriteItem] = _

  protected override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_favorites)
    configureToolbar()
    setNavigationIcon()
    findView(TR.favorite_name_text).setOnEditorActionListener((textView, actionId, event) =>
      if (actionId == EditorInfo.IME_ACTION_SEND ||
        event.getKeyCode == KeyEvent.KEYCODE_ENTER && event.getAction == KeyEvent.ACTION_DOWN) {
        favoritesAdapter.add(new FavoriteItem(textView.getText.toString, MainActivity.instance.getFrequency))
        textView.setText(null)
        true
      } else false)
    val favoriteList = findView(TR.favorite)
    favoriteList.setLayoutManager(new LinearLayoutManager(this))
    favoriteList.setItemAnimator(new DefaultItemAnimator)
    favoritesAdapter = new FavoritesAdapter(findViewById(android.R.id.empty))
    favoriteList.setAdapter(favoritesAdapter)
    undoManager = new UndoSnackbarManager[FavoriteItem](favoriteList, favoritesAdapter.undo)
    new ItemTouchHelper(new SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
      ItemTouchHelper.START | ItemTouchHelper.END) {
      def onSwiped(viewHolder: ViewHolder, direction: Int) = {
        val index = viewHolder.getAdapterPosition
        favoritesAdapter.remove(index)
        undoManager.remove(index, viewHolder.asInstanceOf[FavoriteItemViewHolder].item)
      }
      def onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder) = {
        favoritesAdapter.move(viewHolder.getAdapterPosition, target.getAdapterPosition)
        true
      }
    }).attachToRecyclerView(favoriteList)
  }

  override def onDestroy {
    undoManager.flush
    super.onDestroy
  }
}
