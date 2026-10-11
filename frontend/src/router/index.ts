import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/auctions',
      name: 'auctions',
      component: () => import('../features/auction/views/AuctionsView.vue'),
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
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
