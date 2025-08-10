import { Router } from 'express';
import { parse } from '../controllers/parseController';


const router = Router();
router.get('/run', parse)

export default router;