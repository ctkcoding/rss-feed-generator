import { Router } from 'express';
import { artwork } from '../controllers/artworkController';


const router = Router();
router.get('/:filename', artwork)

export default router;