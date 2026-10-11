import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'auctions',
      component: () => import('../features/auction/views/AuctionsView.vue'),
    },
    {
      // The list used to live here. Kept so old links still work.
      path: '/auctions',
      redirect: '/',
    },
    {
      path: '/auctions/new',
      name: 'auctions-new',
      component: () => import('../features/auction/views/CreateAuctionView.vue'),
    },
    {
      // Static paths such as /auctions/new rank above this one. The view checks the id.
      path: '/auctions/:id',
      name: 'auction-detail',
      component: () => import('../features/auction/views/AuctionDetailView.vue'),
    },
    {
      path: '/me/bids',
      name: 'my-bids',
      component: () => import('../features/bidding/views/MyBidsView.vue'),
    },
  ],
})

export default router
